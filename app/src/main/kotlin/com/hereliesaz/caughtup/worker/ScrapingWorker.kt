package com.hereliesaz.caughtup.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hereliesaz.caughtup.CaughtUpApplication
import com.hereliesaz.caughtup.data.TargetStatus
import com.hereliesaz.caughtup.network.HtmlScraper
import com.hereliesaz.caughtup.network.IdentityVerifier
import com.hereliesaz.caughtup.network.JurisdictionMapper
import com.hereliesaz.caughtup.network.WebViewScraper
import kotlinx.coroutines.flow.first

/**
 * The nocturnal dragnet. Now with object permanence so we don't accidentally bury the living.
 */
class ScrapingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as CaughtUpApplication).container.targetRepository
        val basicScraper = HtmlScraper()
        val stealthScraper = WebViewScraper(applicationContext)
        val mapper = JurisdictionMapper()
        val verifier = IdentityVerifier()
        
        val targetsAtLarge = repository.getTargetsByStatus(TargetStatus.AT_LARGE).first()

        targetsAtLarge.forEach { target ->
            var newStatus = TargetStatus.AT_LARGE

            // 1. Interrogate the municipal cages
            val lockupUrl = mapper.getLockupUrl(target.areaCode, target.residenceInfo)
            if (lockupUrl != null) {
                val isIncarcerated = basicScraper.scrapeMugshots(lockupUrl, target.displayName)
                if (isIncarcerated) {
                    newStatus = TargetStatus.INCARCERATED
                }
            }

            // 2. If they aren't in a cell, check if they are in the ground
            if (newStatus == TargetStatus.AT_LARGE) {
                val obitUrl = mapper.getObituaryUrl(target.areaCode, target.residenceInfo)
                if (obitUrl != null) {
                    val obitDoc = stealthScraper.scrapeGhostTown(obitUrl)
                    val textToSearch = obitDoc?.text() ?: ""
                    
                    if (verifier.verifyIdentity(textToSearch, target.displayName)) {
                        newStatus = TargetStatus.DECEASED
                    }
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

        return Result.success()
    }
}
