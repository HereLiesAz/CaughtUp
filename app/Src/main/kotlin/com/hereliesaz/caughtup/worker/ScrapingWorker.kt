package com.hereliesaz.caughtup.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hereliesaz.caughtup.CaughtUpApplication
import com.hereliesaz.caughtup.data.TargetStatus
import com.hereliesaz.caughtup.network.HtmlScraper
import com.hereliesaz.caughtup.network.JurisdictionMapper
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
        val mapper = JurisdictionMapper()
        
        val targetsAtLarge = repository.getTargetsByStatus(TargetStatus.AT_LARGE).first()

        targetsAtLarge.forEach { target ->
            val lockupUrl = mapper.getLockupUrl(target.areaCode)
            
            if (lockupUrl != null) {
                val isCaughtUp = scraper.scrapeMugshots(lockupUrl, target.displayName)
                
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
        }

        return Result.success()
    }
}

