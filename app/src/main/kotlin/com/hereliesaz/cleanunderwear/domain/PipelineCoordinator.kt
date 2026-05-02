package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single-flight guard for any operation that walks the registry —
 * the auto pipeline, deduplication, triage, scrape, and user-initiated
 * cyberbackgroundchecks enrichment runs.
 *
 * If a run is in flight when another caller arrives, the second caller
 * **queues** until the first finishes. Operations never parallelize, never
 * refuse: just wait their turn. This honors the user's "queue, don't drop"
 * rule for user-initiated runs and prevents two pipeline ticks from
 * fighting over the same Target rows.
 */
@Singleton
class PipelineCoordinator @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> runExclusive(label: String, block: suspend () -> T): T {
        if (mutex.isLocked) {
            DiagnosticLogger.log("PipelineCoordinator: '$label' queued — another job in flight")
        }
        return mutex.withLock {
            DiagnosticLogger.log("PipelineCoordinator: '$label' running")
            try {
                block()
            } finally {
                DiagnosticLogger.log("PipelineCoordinator: '$label' done")
            }
        }
    }

    val isBusy: Boolean
        get() = mutex.isLocked
}
