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
import kotlinx.coroutines.flow.first
import javax.inject.Inject

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
 
class ScrapeTargetsUseCase @Inject constructor(
    private val repository: TargetRepository,
    private val basicScraper: HtmlScraper,
    private val stealthScraper: WebViewScraper,
    private val researchAgent: OnDeviceResearchAgent,
    private val verifier: IdentityVerifier,
    private val notifications: NotificationHelper,
    private val contactSyncer: SystemContactSyncer
) {
    private val semaphore = Semaphore(3) // Only 3 concurrent "interrogations" to avoid detection

    suspend operator fun invoke(onProgress: (Float, String) -> Unit = { _, _ -> }) = coroutineScope {
        val allTargets = repository.getAllTargets().first()
        val now = System.currentTimeMillis()
        
        val targetsToProcess = allTargets.filter { 
            it.status != TargetStatus.IGNORED && it.nextScheduledCheck <= now 
        }
        
        if (targetsToProcess.isEmpty()) {
            onProgress(1f, "Registry is up to date.")
            return@coroutineScope
        }

        val total = targetsToProcess.size
        var completedCount = 0

        targetsToProcess.map { target ->
            async {
                semaphore.withPermit {
                    completedCount++
                    onProgress(completedCount.toFloat() / total, "Interrogating ${target.displayName}...")
                    processTarget(target, now)
                }
            }
        }.awaitAll()
        
        onProgress(1f, "Daily vigil completed.")
    }

    private suspend fun processTarget(target: Target, now: Long) {
        try {
            var activeTarget = target

            // 0. Intelligence Enrichment (Field Population)
            // If the target is nameless or missing residence info, attempt to resolve via public records
            if (target.displayName == "Unnamed Entity" || target.residenceInfo.isNullOrBlank()) {
                activeTarget = researchAgent.enrichIntelligence(target)
            }

            var newStatus = TargetStatus.MONITORING
            var discoveredLockupUrl = activeTarget.lockupUrl
            var discoveredObitUrl = activeTarget.obituaryUrl
            var verificationSnippet = activeTarget.lastVerificationSnippet

            // 1. Interrogate the municipal cages
            val lockupUrl = activeTarget.lockupUrl ?: researchAgent.getDynamicLockupUrl(activeTarget.areaCode, activeTarget.residenceInfo).also {
                discoveredLockupUrl = it
            }
            
            DiagnosticLogger.log("Checking jail roster for ${activeTarget.displayName} at $lockupUrl")
            
            // Note: Updated HtmlScraper to return result
            val lockupResult = basicScraper.scrapeMugshots(lockupUrl, activeTarget.displayName)
            if (lockupResult.isMatch) {
                DiagnosticLogger.log("MATCH FOUND: ${activeTarget.displayName} located in local jail.", DiagnosticLogger.LogEntry.LogLevel.WARN)
                newStatus = TargetStatus.INCARCERATED
                verificationSnippet = lockupResult.snippet
            }

            // 2. If they aren't in a cell, check if they are in the ground
            if (newStatus == TargetStatus.MONITORING) {
                val obitUrl = activeTarget.obituaryUrl ?: researchAgent.getDynamicObituaryUrl(activeTarget.areaCode, activeTarget.residenceInfo).also {
                    discoveredObitUrl = it
                }
                
                DiagnosticLogger.log("Checking obituary registry for ${activeTarget.displayName} at $obitUrl")
                
                val obitDoc = stealthScraper.scrapeGhostTown(obitUrl)
                if (obitDoc != null) {
                    val result = verifier.verifyIdentity(obitDoc.text(), activeTarget.displayName)
                    if (result.isMatch) {
                        DiagnosticLogger.log("DEATH DETECTED: ${activeTarget.displayName} found in obituary registry.", DiagnosticLogger.LogEntry.LogLevel.WARN)
                        newStatus = TargetStatus.DECEASED
                        verificationSnippet = result.snippet
                    }
                }
            }

            // 3. Detect Status Change and Notify
            var statusChangeTimestamp = activeTarget.lastStatusChangeTimestamp
            if (newStatus != activeTarget.status && activeTarget.status != TargetStatus.UNKNOWN) {
                notifications.notifyStatusChange(activeTarget.copy(status = newStatus), activeTarget.status)
                statusChangeTimestamp = now
            }

            // 4. Update the ledger
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
            
            // 5. Sync back to system contacts
            contactSyncer.syncToSystem(updatedTarget)
        } catch (e: Exception) {
            android.util.Log.e("ScrapeUseCase", "The void refused to yield data for ${target.displayName}", e)
        }
    }
}
