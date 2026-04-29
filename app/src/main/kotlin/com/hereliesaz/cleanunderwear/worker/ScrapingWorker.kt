package com.hereliesaz.cleanunderwear.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hereliesaz.cleanunderwear.domain.ScrapeTargetsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * The nocturnal dragnet. Now with object permanence so we don't accidentally bury the living.
 */
class ScrapingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ScrapingWorkerEntryPoint {
        fun scrapeTargetsUseCase(): ScrapeTargetsUseCase
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ScrapingWorkerEntryPoint::class.java
        )
        val scrapeTargetsUseCase = entryPoint.scrapeTargetsUseCase()

        scrapeTargetsUseCase { progress, description ->
            setProgressAsync(workDataOf("progress" to progress, "description" to description))
        }

        return Result.success()
    }
}
