package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.data.TargetStatus
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.network.HtmlScraper
import com.hereliesaz.cleanunderwear.network.IdentityVerifier
import com.hereliesaz.cleanunderwear.network.OnDeviceResearchAgent
import com.hereliesaz.cleanunderwear.network.WebViewScraper
import com.hereliesaz.cleanunderwear.ui.NotificationHelper
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import com.hereliesaz.cleanunderwear.util.SystemContactSyncer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Phase 4 of the pipeline: actually monitor the READY targets against public records.
 *
 * For each target there are six observable sub-steps; we emit progress + a description after
 * each one so the UI can show "47/123 · Alice Smith · checking obituary registry". The bar
 * fraction is `completedTargets / totalTargets` — sub-steps don't push the bar forward but they
 * do refresh the description.
 */
class ScrapeTargetsUseCase @Inject constructor(
    private val repository: TargetRepository,
    private val basicScraper: HtmlScraper,
    private val stealthScraper: WebViewScraper,
    private val researchAgent: OnDeviceResearchAgent,
    private val verifier: IdentityVerifier,
    private val notifications: NotificationHelper,
    private val contactSyncer: SystemContactSyncer
) {
    private val semaphore = Semaphore(3)

    suspend operator fun invoke(onProgress: (Float, String) -> Unit = { _, _ -> }) = coroutineScope {
        val now = System.currentTimeMillis()
        val targetsToProcess = repository.getReadyDueTargets(now)

        if (targetsToProcess.isEmpty()) {
            onProgress(1f, "Registry is up to date")
            return@coroutineScope
        }

        val total = targetsToProcess.size
        val completionLock = Mutex()
        var completedCount = 0

        val emitMutex = Mutex()
        suspend fun emit(currentName: String, step: String) {
            emitMutex.withLock {
                val frac = completedCount.toFloat() / total
                onProgress(frac, "${completedCount + 1}/$total · $currentName · $step")
            }
        }

        targetsToProcess.map { target ->
            async {
                semaphore.withPermit {
                    processTarget(target, now) { step -> emit(target.displayName, step) }
                    completionLock.withLock {
                        completedCount++
                    }
                    onProgress(
                        completedCount.toFloat() / total,
                        "$completedCount/$total · ${target.displayName} · done"
                    )
                }
            }
        }.awaitAll()

        onProgress(1f, "Daily vigil completed")
    }

    private suspend fun processTarget(
        target: Target,
        now: Long,
        emitStep: suspend (String) -> Unit
    ) {
        try {
            var activeTarget = target

            if (target.displayName == "Unnamed Entity" || target.residenceInfo.isNullOrBlank()) {
                emitStep("enriching missing intel")
                activeTarget = researchAgent.enrichIntelligence(target)
            }

            var newStatus = TargetStatus.MONITORING
            var discoveredLockupUrl = activeTarget.lockupUrl
            var discoveredObitUrl = activeTarget.obituaryUrl
            var verificationSnippet = activeTarget.lastVerificationSnippet

            emitStep("resolving jail roster URL")
            val lockupUrl = activeTarget.lockupUrl
                ?: researchAgent.getDynamicLockupUrl(activeTarget.areaCode, activeTarget.residenceInfo)
                    .also { discoveredLockupUrl = it }

            emitStep("checking jail roster")
            DiagnosticLogger.log("Checking jail roster for ${activeTarget.displayName} at $lockupUrl")
            val lockupResult = basicScraper.scrapeMugshots(lockupUrl, activeTarget.displayName)
            if (lockupResult.isMatch) {
                DiagnosticLogger.log(
                    "MATCH FOUND: ${activeTarget.displayName} located in local jail.",
                    DiagnosticLogger.LogEntry.LogLevel.WARN
                )
                newStatus = TargetStatus.INCARCERATED
                verificationSnippet = lockupResult.snippet
            }

            if (newStatus == TargetStatus.MONITORING) {
                emitStep("resolving obituary registry URL")
                val obitUrl = activeTarget.obituaryUrl
                    ?: researchAgent.getDynamicObituaryUrl(activeTarget.areaCode, activeTarget.residenceInfo)
                        .also { discoveredObitUrl = it }

                emitStep("checking obituary registry")
                DiagnosticLogger.log("Checking obituary registry for ${activeTarget.displayName} at $obitUrl")
                val obitDoc = stealthScraper.scrapeGhostTown(obitUrl)
                if (obitDoc != null) {
                    val result = verifier.verifyIdentity(obitDoc.text(), activeTarget.displayName)
                    if (result.isMatch) {
                        DiagnosticLogger.log(
                            "DEATH DETECTED: ${activeTarget.displayName} found in obituary registry.",
                            DiagnosticLogger.LogEntry.LogLevel.WARN
                        )
                        newStatus = TargetStatus.DECEASED
                        verificationSnippet = result.snippet
                    }
                }
            }

            var statusChangeTimestamp = activeTarget.lastStatusChangeTimestamp
            if (newStatus != activeTarget.status && activeTarget.status != TargetStatus.UNKNOWN) {
                emitStep("notifying status change")
                notifications.notifyStatusChange(activeTarget.copy(status = newStatus), activeTarget.status)
                statusChangeTimestamp = now
            }

            emitStep("persisting findings")
            val updatedTarget = activeTarget.copy(
                status = newStatus,
                lastScrapedTimestamp = now,
                lockupUrl = discoveredLockupUrl,
                obituaryUrl = discoveredObitUrl,
                nextScheduledCheck = now + (activeTarget.checkFrequencyHours * 3600000L),
                lastStatusChangeTimestamp = statusChangeTimestamp,
                lastVerificationSnippet = verificationSnippet
            )
            repository.updateTarget(updatedTarget)

            emitStep("syncing back to system contacts")
            contactSyncer.syncToSystem(updatedTarget)
        } catch (e: Exception) {
            android.util.Log.e("ScrapeUseCase", "The void refused to yield data for ${target.displayName}", e)
        }
    }
}
