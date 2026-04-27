package com.hereliesaz.caughtup

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hereliesaz.caughtup.data.AppContainer
import com.hereliesaz.caughtup.data.DefaultAppContainer
import com.hereliesaz.caughtup.worker.ScrapingWorker
import java.util.concurrent.TimeUnit

class CaughtUpApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        scheduleDragnet()
    }

    private fun scheduleDragnet() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<ScrapingWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_incarceration_sweep",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
