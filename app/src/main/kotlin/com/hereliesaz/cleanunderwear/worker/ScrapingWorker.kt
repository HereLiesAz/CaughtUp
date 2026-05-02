package com.hereliesaz.cleanunderwear.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hereliesaz.cleanunderwear.domain.ScrapeTargetsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The nocturnal dragnet. Now with object permanence so we don't accidentally bury the living.
 */
@HiltWorker
class ScrapingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scrapeTargetsUseCase: ScrapeTargetsUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        scrapeTargetsUseCase { progress, description ->
            setProgressAsync(workDataOf("progress" to progress, "description" to description))
        }

        return Result.success()
    }
}
