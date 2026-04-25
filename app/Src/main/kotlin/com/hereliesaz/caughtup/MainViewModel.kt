package com.hereliesaz.caughtup.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hereliesaz.caughtup.CaughtUpApplication
import com.hereliesaz.caughtup.data.ContactHarvester
import com.hereliesaz.caughtup.data.Target
import com.hereliesaz.caughtup.worker.ScrapingWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The puppet master. Now capable of forcing the issue for the chronically impatient.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CaughtUpApplication).container.targetRepository
    private val harvester = ContactHarvester(application.contentResolver)
    private val workManager = WorkManager.getInstance(application)

    val targets: StateFlow<List<Target>> = repository.getAllTargets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun sweepContacts() {
        viewModelScope.launch {
            val freshMeat = harvester.harvestContacts()
            // The database handles the conflicts. We just dump the bodies in.
            repository.insertTargets(freshMeat)
        }
    }

    fun triggerManualInterrogation() {
        val request = OneTimeWorkRequestBuilder<ScrapingWorker>().build()
        workManager.enqueue(request)
    }
}
