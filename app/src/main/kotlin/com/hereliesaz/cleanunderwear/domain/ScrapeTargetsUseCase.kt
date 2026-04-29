package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.TargetStatus
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.network.HtmlScraper
import com.hereliesaz.cleanunderwear.network.IdentityVerifier
import com.hereliesaz.cleanunderwear.network.OnDeviceResearchAgent
import com.hereliesaz.cleanunderwear.network.WebViewScraper
import com.hereliesaz.cleanunderwear.ui.NotificationHelper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ScrapeTargetsUseCase @Inject constructor(
    private val repository: TargetRepository,
    private val basicScraper: HtmlScraper,
    private val stealthScraper: WebViewScraper,
    private val researchAgent: OnDeviceResearchAgent,
    private val verifier: IdentityVerifier,
    private val notifications: NotificationHelper
) {
    suspend operator fun invoke() {
        val allTargets = repository.getAllTargets().first()
        val now = System.currentTimeMillis()

        allTargets.forEach { target ->
            // Skip ignored targets and those not yet due
            if (target.status == TargetStatus.IGNORED || target.nextScheduledCheck > now) return@forEach

            var newStatus = TargetStatus.AT_LARGE
            var discoveredLockupUrl = target.lockupUrl
            var discoveredObitUrl = target.obituaryUrl

            // 1. Interrogate the municipal cages
            val lockupUrl = target.lockupUrl ?: researchAgent.getDynamicLockupUrl(target.areaCode, target.residenceInfo).also {
                discoveredLockupUrl = it
            }
            val isIncarcerated = basicScraper.scrapeMugshots(lockupUrl, target.displayName)
            if (isIncarcerated) {
                newStatus = TargetStatus.INCARCERATED
            }

            // 2. If they aren't in a cell, check if they are in the ground
            var verificationSnippet = target.lastVerificationSnippet
            
            if (newStatus == TargetStatus.AT_LARGE) {
                val obitUrl = target.obituaryUrl ?: researchAgent.getDynamicObituaryUrl(target.areaCode, target.residenceInfo).also {
                    discoveredObitUrl = it
                }
                val obitDoc = stealthScraper.scrapeGhostTown(obitUrl)
                val textToSearch = obitDoc?.text() ?: ""
                
                val result = verifier.verifyIdentity(textToSearch, target.displayName)
                if (result.isMatch) {
                    newStatus = TargetStatus.DECEASED
                    verificationSnippet = result.snippet
                }
            }

            // 3. Detect Status Change and Notify
            var statusChangeTimestamp = target.lastStatusChangeTimestamp
            if (newStatus != target.status && target.status != TargetStatus.UNKNOWN) {
                notifications.notifyStatusChange(target.copy(status = newStatus), target.status)
                statusChangeTimestamp = now
            }

            // 4. Update the ledger
            repository.updateTarget(
                target.copy(
                    status = newStatus,
                    lastScrapedTimestamp = now,
                    lockupUrl = discoveredLockupUrl,
                    obituaryUrl = discoveredObitUrl,
                    nextScheduledCheck = now + (target.checkFrequencyHours * 3600000L),
                    lastStatusChangeTimestamp = statusChangeTimestamp,
                    lastVerificationSnippet = verificationSnippet
                )
            )
        }
    }
}
