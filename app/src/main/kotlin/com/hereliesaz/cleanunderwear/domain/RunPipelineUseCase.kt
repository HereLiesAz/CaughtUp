package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import javax.inject.Inject

/**
 * Top-level orchestrator. Runs the four sequential pipeline phases and maps each phase's
 * sub-progress onto the overall (Float, String) callback consumed by ScrapingWorker / the UI.
 *
 *   Phase 1/4 · Deduplicating
 *   Phase 2/4 · Triaging
 *   Phase 3/4 · Enriching
 *   Phase 4/4 · Monitoring
 *
 * Phase 3 only runs when Triage flagged any NEEDS_ENRICHMENT targets; otherwise we still emit
 * a "skipped" beat so the bar advances cleanly.
 */
class RunPipelineUseCase @Inject constructor(
    private val deduplicate: DeduplicateTargetsUseCase,
    private val triage: TriageTargetsUseCase,
    private val enrich: EnrichTargetsUseCase,
    private val scrape: ScrapeTargetsUseCase
) {
    private val phases = listOf("Deduplicating", "Triaging", "Enriching", "Monitoring")

    suspend operator fun invoke(onProgress: (Float, String) -> Unit) {
        val pipeline = PipelineProgress(phases, onProgress)

        pipeline.beginPhase("Deduplicating")
        val merged = deduplicate { sub, desc -> pipeline.emit(sub, desc) }
        DiagnosticLogger.log("Pipeline: dedup merged $merged duplicate row(s)")

        pipeline.beginPhase("Triaging")
        val triageResult = triage { sub, desc -> pipeline.emit(sub, desc) }
        DiagnosticLogger.log(
            "Pipeline: triage → ${triageResult.ready} ready / ${triageResult.needsEnrichment} need enrichment"
        )

        pipeline.beginPhase("Enriching")
        if (triageResult.needsEnrichment == 0) {
            pipeline.emit(1f, "no targets need enrichment")
        } else {
            enrich { sub, desc -> pipeline.emit(sub, desc) }
        }

        pipeline.beginPhase("Monitoring")
        scrape { sub, desc -> pipeline.emit(sub, desc) }

        pipeline.completeAll("Pipeline complete")
    }
}

/**
 * Helper to map sub-progress of individual phases onto a global 0.0 - 1.0 scale.
 */
internal class PipelineProgress(
    private val phases: List<String>,
    private val onProgress: (Float, String) -> Unit
) {
    private var currentPhaseIndex = -1

    fun beginPhase(name: String) {
        currentPhaseIndex = phases.indexOf(name)
    }

    fun emit(subProgress: Float, description: String) {
        if (currentPhaseIndex == -1) return
        val totalPhases = phases.size
        val overallProgress = (currentPhaseIndex + subProgress) / totalPhases
        val phaseLabel = "Phase ${currentPhaseIndex + 1}/$totalPhases · ${phases[currentPhaseIndex]}"
        onProgress(overallProgress, "$phaseLabel · $description")
    }

    fun completeAll(description: String) {
        onProgress(1f, description)
    }
}
