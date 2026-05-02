package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.MonitorabilityState
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.network.CyberBackgroundChecksEnricher
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import javax.inject.Inject

/**
 * Walks every NEEDS_ENRICHMENT target (loaded as a bounded subset, not the full registry) and
 * runs it through cyberbackgroundchecks. Successful enrichments are persisted with READY state;
 * failures are persisted with ENRICHMENT_FAILED so the next pipeline run skips them.
 */
class EnrichTargetsUseCase @Inject constructor(
    private val repository: TargetRepository,
    private val enricher: CyberBackgroundChecksEnricher
) {
    data class EnrichmentResult(val attempted: Int, val succeeded: Int, val failed: Int)

    suspend operator fun invoke(onProgress: (Float, String) -> Unit = { _, _ -> }): EnrichmentResult {
        val pending = repository.getTargetsByMonitorability(MonitorabilityState.NEEDS_ENRICHMENT)
        if (pending.isEmpty()) {
            onProgress(1f, "Nothing to enrich")
            return EnrichmentResult(0, 0, 0)
        }

        val total = pending.size
        var succeeded = 0
        var failed = 0

        for ((index, target) in pending.withIndex()) {
            onProgress(
                index.toFloat() / total,
                "${index + 1}/$total · ${target.displayName} · cybg lookup"
            )

            val enriched = try {
                enricher.enrich(target)
            } catch (e: Exception) {
                DiagnosticLogger.log(
                    "Enrichment error for ${target.displayName}: ${e.message}",
                    DiagnosticLogger.LogEntry.LogLevel.WARN
                )
                null
            }

            if (enriched != null) {
                val hasRealName = enriched.displayName.isNotBlank() &&
                    enriched.displayName != "Unnamed Entity" &&
                    !enriched.displayName.startsWith("Unnamed Entity (")
                val hasPhone = (enriched.phoneNumber?.filter { it.isDigit() }?.length ?: 0) >= 10
                val hasLocation = !enriched.residenceInfo.isNullOrBlank() ||
                    (!enriched.areaCode.isNullOrBlank() && enriched.areaCode != "LOCAL")

                val newState = if (hasRealName && (hasPhone || hasLocation))
                    MonitorabilityState.READY
                else
                    MonitorabilityState.ENRICHMENT_FAILED

                repository.updateTarget(enriched.copy(monitorabilityState = newState))
                if (newState == MonitorabilityState.READY) succeeded++ else failed++
            } else {
                repository.updateMonitorabilityState(target.id, MonitorabilityState.ENRICHMENT_FAILED)
                failed++
            }

            onProgress(
                (index + 1).toFloat() / total,
                "${index + 1}/$total · ${target.displayName} · ${if (enriched != null) "enriched" else "no data"}"
            )
        }

        DiagnosticLogger.log("Enrichment: $succeeded ready, $failed failed (out of $total)")
        return EnrichmentResult(attempted = total, succeeded = succeeded, failed = failed)
    }
}
