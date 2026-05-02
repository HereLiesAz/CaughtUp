package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.MonitorabilityState
import com.hereliesaz.cleanunderwear.data.TargetLite
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.data.TargetStatus
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import javax.inject.Inject

/**
 * Walks every Target (via the lite projection — full rows are not loaded) and decides whether
 * it has the minimum identifying data to be monitored against public records, or whether it
 * should be queued for cyberbackgroundchecks enrichment.
 *
 * Rule for READY:
 *   - displayName is a real name (not "Unnamed Entity" / "Unnamed Entity (...)"), AND
 *   - has at least one of: phone number with >=10 digits OR (residenceInfo or areaCode != "LOCAL")
 *
 * Anything else moves to NEEDS_ENRICHMENT (unless it was already ENRICHMENT_FAILED, in which
 * case we leave it alone so we don't churn cybg lookups every cycle).
 *
 * IGNORED targets are skipped — the user explicitly archived them.
 *
 * State changes are persisted via a targeted UPDATE on the monitorability column only — no
 * full-row read or write is needed.
 */
class TriageTargetsUseCase @Inject constructor(
    private val repository: TargetRepository
) {
    data class TriageResult(val ready: Int, val needsEnrichment: Int, val ignored: Int)

    suspend operator fun invoke(onProgress: (Float, String) -> Unit = { _, _ -> }): TriageResult {
        val all = repository.getAllTargetsLiteSnapshot()
        if (all.isEmpty()) {
            onProgress(1f, "No targets to triage")
            return TriageResult(0, 0, 0)
        }

        var ready = 0
        var needsEnrichment = 0
        var ignored = 0
        val total = all.size

        for ((index, target) in all.withIndex()) {
            if (target.status == TargetStatus.IGNORED) {
                ignored++
                onProgress((index + 1).toFloat() / total, "Skipping archived: ${target.displayName}")
                continue
            }

            val newState = decide(target)

            if (newState != target.monitorabilityState) {
                repository.updateMonitorabilityState(target.id, newState)
            }

            when (newState) {
                MonitorabilityState.READY -> ready++
                MonitorabilityState.NEEDS_ENRICHMENT, MonitorabilityState.ENRICHMENT_FAILED -> needsEnrichment++
            }

            onProgress(
                (index + 1).toFloat() / total,
                "${index + 1}/$total · ${target.displayName}"
            )
        }

        DiagnosticLogger.log("Triage: $ready ready, $needsEnrichment need enrichment, $ignored archived")
        return TriageResult(ready, needsEnrichment, ignored)
    }

    private fun decide(target: TargetLite): MonitorabilityState {
        val hasRealName = isRealName(target.displayName)
        val hasPhone = (target.phoneNumber?.filter { it.isDigit() }?.length ?: 0) >= 10
        val hasLocation = !target.residenceInfo.isNullOrBlank() ||
            (!target.areaCode.isNullOrBlank() && target.areaCode != "LOCAL")

        return if (hasRealName && (hasPhone || hasLocation)) {
            MonitorabilityState.READY
        } else if (target.monitorabilityState == MonitorabilityState.ENRICHMENT_FAILED) {
            MonitorabilityState.ENRICHMENT_FAILED
        } else {
            MonitorabilityState.NEEDS_ENRICHMENT
        }
    }

    private fun isRealName(name: String): Boolean {
        if (name.isBlank()) return false
        if (name == "Unnamed Entity") return false
        if (name.startsWith("Unnamed Entity (")) return false
        return true
    }
}
