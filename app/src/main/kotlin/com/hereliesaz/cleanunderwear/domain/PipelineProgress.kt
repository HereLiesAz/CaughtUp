package com.hereliesaz.cleanunderwear.domain

/**
 * Maps sub-task progress emitted by an individual use case onto the overall pipeline timeline.
 *
 * Phases are weighted equally by default (1/N each). Each use case calls [emit] with a 0..1
 * fraction within its own phase plus an optional human-readable description. Consumers receive
 * the weighted overall fraction with a phase-prefixed description, e.g.:
 *
 *   "Phase 2/4 · Triaging · 47/123 · Alice Smith"
 *
 * This keeps the (Float, String) callback signature already used by ScrapingWorker.
 */
class PipelineProgress(
    private val phaseLabels: List<String>,
    private val onUpdate: (Float, String) -> Unit
) {
    private var currentPhaseIndex: Int = 0
    private val phaseShare: Float get() = 1f / phaseLabels.size.coerceAtLeast(1)

    fun beginPhase(phaseLabel: String) {
        val idx = phaseLabels.indexOf(phaseLabel)
        require(idx >= 0) { "Unknown phase: $phaseLabel (known: $phaseLabels)" }
        currentPhaseIndex = idx
        emit(0f, phaseLabel)
    }

    /**
     * @param subFraction 0..1 within the current phase
     * @param description optional sub-task description appended after the phase label
     */
    fun emit(subFraction: Float, description: String) {
        val clamped = subFraction.coerceIn(0f, 1f)
        val overall = (currentPhaseIndex * phaseShare) + clamped * phaseShare
        val phase = phaseLabels[currentPhaseIndex]
        val label = "Phase ${currentPhaseIndex + 1}/${phaseLabels.size} · $phase · $description"
        onUpdate(overall.coerceIn(0f, 1f), label)
    }

    fun completeAll(finalDescription: String) {
        onUpdate(1f, finalDescription)
    }
}
