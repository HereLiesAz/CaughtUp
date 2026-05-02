package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.MonitorabilityState
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.data.TargetStatus
import com.hereliesaz.cleanunderwear.data.TargetWorkInfo
import com.hereliesaz.cleanunderwear.network.NameValidator
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Walks every Target (via the lite projection — full rows are not loaded) and decides whether
 * it has the minimum identifying data to be monitored against public records, or whether it
 * should be queued for cyberbackgroundchecks enrichment.
 *
 * Parallelized to handle large registries (8000+ contacts) efficiently.
 */
class TriageTargetsUseCase @Inject constructor(
    private val repository: TargetRepository
) {
    data class TriageResult(val ready: Int, val needsEnrichment: Int, val ignored: Int)

    private data class Decision(
        val state: MonitorabilityState,
        val newStatus: TargetStatus?
    )

    suspend operator fun invoke(onProgress: (Float, String) -> Unit = { _, _ -> }): TriageResult = coroutineScope {
        var readyCount = 0
        var needsEnrichmentCount = 0
        var ignoredCount = 0

        var offset = 0
        val chunkSize = 1000

        while (true) {
            val chunk = repository.getTargetWorkInfoPaged(chunkSize, offset)
            if (chunk.isEmpty()) break

            val totalProcessed = offset + chunk.size
            onProgress(0.1f, "Triaging contacts ($totalProcessed)...")

            val decisions = withContext(Dispatchers.Default) {
                chunk.map { target ->
                    async {
                        if (target.status == TargetStatus.IGNORED) {
                            target.id to null
                        } else {
                            target.id to decide(target)
                        }
                    }
                }.awaitAll()
            }

            val toReady = mutableListOf<Int>()
            val toNeedsEnrichment = mutableListOf<Int>()
            val toEnrichmentFailed = mutableListOf<Int>()
            val statusUpdates = mutableListOf<Pair<Int, TargetStatus>>()

            decisions.forEach { (id, decision) ->
                val original = chunk.find { it.id == id } ?: return@forEach

                if (decision == null) {
                    ignoredCount++
                } else {
                    if (decision.state != original.monitorabilityState) {
                        when (decision.state) {
                            MonitorabilityState.READY -> toReady.add(id)
                            MonitorabilityState.NEEDS_ENRICHMENT -> toNeedsEnrichment.add(id)
                            MonitorabilityState.ENRICHMENT_FAILED -> toEnrichmentFailed.add(id)
                        }
                    }
                    if (decision.newStatus != null && decision.newStatus != original.status) {
                        statusUpdates.add(id to decision.newStatus)
                    }
                    if (decision.state == MonitorabilityState.READY) readyCount++ else needsEnrichmentCount++
                }
            }

            if (toReady.isNotEmpty()) {
                repository.updateMonitorabilityStateBatch(toReady, MonitorabilityState.READY)
            }
            if (toNeedsEnrichment.isNotEmpty()) {
                repository.updateMonitorabilityStateBatch(toNeedsEnrichment, MonitorabilityState.NEEDS_ENRICHMENT)
            }
            if (toEnrichmentFailed.isNotEmpty()) {
                repository.updateMonitorabilityStateBatch(toEnrichmentFailed, MonitorabilityState.ENRICHMENT_FAILED)
            }
            if (statusUpdates.isNotEmpty()) {
                val now = System.currentTimeMillis()
                statusUpdates.forEach { (id, status) ->
                    repository.updateStatusOnly(id, status, now)
                }
            }

            offset += chunkSize
        }

        DiagnosticLogger.log("Triage (Chunked): $readyCount ready, $needsEnrichmentCount need enrichment, $ignoredCount archived")
        onProgress(1f, "Triage complete.")
        TriageResult(readyCount, needsEnrichmentCount, ignoredCount)
    }

    private fun decide(target: TargetWorkInfo): Decision {
        val nameVerifiable = NameValidator.isVerifiable(target.displayName)
        val hasPhone = (target.phoneNumber?.filter { it.isDigit() }?.length ?: 0) >= 10
        val hasLocation = !target.residenceInfo.isNullOrBlank() ||
            (!target.areaCode.isNullOrBlank() && target.areaCode != "LOCAL")

        return when {
            nameVerifiable && (hasPhone || hasLocation) -> {
                val statusReset = if (target.status == TargetStatus.UNVERIFIED) {
                    TargetStatus.MONITORING
                } else null
                Decision(MonitorabilityState.READY, statusReset)
            }
            !nameVerifiable -> {
                val nextState = if (target.monitorabilityState == MonitorabilityState.ENRICHMENT_FAILED) {
                    MonitorabilityState.ENRICHMENT_FAILED
                } else {
                    MonitorabilityState.NEEDS_ENRICHMENT
                }
                val statusOverride = if (target.status == TargetStatus.MONITORING ||
                    target.status == TargetStatus.UNKNOWN
                ) {
                    TargetStatus.UNVERIFIED
                } else null
                Decision(nextState, statusOverride)
            }
            else -> {
                val nextState = if (target.monitorabilityState == MonitorabilityState.ENRICHMENT_FAILED) {
                    MonitorabilityState.ENRICHMENT_FAILED
                } else {
                    MonitorabilityState.NEEDS_ENRICHMENT
                }
                Decision(nextState, null)
            }
        }
    }
}
