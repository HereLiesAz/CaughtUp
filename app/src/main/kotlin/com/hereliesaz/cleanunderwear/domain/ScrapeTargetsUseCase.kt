package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.TargetStatus
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.network.HtmlScraper
import com.hereliesaz.cleanunderwear.network.IdentityVerifier
import com.hereliesaz.cleanunderwear.network.OnDeviceResearchAgent
import com.hereliesaz.cleanunderwear.network.WebViewScraper
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ScrapeTargetsUseCase @Inject constructor(
    private val repository: TargetRepository,
    private val basicScraper: HtmlScraper,
    private val stealthScraper: WebViewScraper,
    private val researchAgent: OnDeviceResearchAgent,
    private val verifier: IdentityVerifier
) {
    suspend operator fun invoke() {
        val targetsAtLarge = repository.getTargetsByStatus(TargetStatus.AT_LARGE).first()

        targetsAtLarge.forEach { target ->
            var newStatus = TargetStatus.AT_LARGE

            // 1. Interrogate the municipal cages dynamically using the research agent
            val lockupUrl = researchAgent.getDynamicLockupUrl(target.areaCode, target.residenceInfo)
            val isIncarcerated = basicScraper.scrapeMugshots(lockupUrl, target.displayName)
            if (isIncarcerated) {
                newStatus = TargetStatus.INCARCERATED
            }

            // 2. If they aren't in a cell, check if they are in the ground
            if (newStatus == TargetStatus.AT_LARGE) {
                val obitUrl = researchAgent.getDynamicObituaryUrl(target.areaCode, target.residenceInfo)
                val obitDoc = stealthScraper.scrapeGhostTown(obitUrl)
                val textToSearch = obitDoc?.text() ?: ""
                
                if (verifier.verifyIdentity(textToSearch, target.displayName)) {
                    newStatus = TargetStatus.DECEASED
                }
            }

            // Update the ledger with our grim findings
            repository.updateTarget(
                target.copy(
                    status = newStatus,
                    lastScrapedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
