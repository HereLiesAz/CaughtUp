package com.hereliesaz.caughtup.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hereliesaz.caughtup.CaughtUpApplication
import com.hereliesaz.caughtup.data.TargetStatus
import com.hereliesaz.caughtup.network.HtmlScraper
import kotlinx.coroutines.flow.first

/**
 * The nocturnal dragnet. Wakes up when the OS allows it, grabs the ledger, 
 * and sends the buzzards out to check the local lockups.
 */
class ScrapingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as CaughtUpApplication).container.targetRepository
        val scraper = HtmlScraper()
        
        // For now, we only investigate those who are theoretically free.
        val targetsAtLarge = repository.getTargetsByStatus(TargetStatus.AT_LARGE).first()

        targetsAtLarge.forEach { target ->
            // TODO: Map area code to specific municipal URL. 
            // For now, we simulate the interrogation against a hypothetical endpoint.
            val mockJurisdictionUrl = "https://example-county-sheriff.gov/roster"
            
            val isCaughtUp = scraper.scrapeMugshots(mockJurisdictionUrl, target.displayName)
            
            if (isCaughtUp) {
                repository.updateTarget(
                    target.copy(
                        status = TargetStatus.INCARCERATED,
                        lastScrapedTimestamp = System.currentTimeMillis()
                    )
                )
            } else {
                repository.updateTarget(
                    target.copy(
                        lastScrapedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        return Result.success()
    }
}

