package com.hereliesaz.cleanunderwear.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.data.TargetStatus
import com.hereliesaz.cleanunderwear.domain.HarvestContactsUseCase
import com.hereliesaz.cleanunderwear.domain.SetupSourcesUseCase
import com.hereliesaz.cleanunderwear.worker.ScrapingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.core.content.edit

/**
 * The puppet master. Now capable of forcing the issue for the chronically impatient.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val repository: TargetRepository,
    private val harvestContactsUseCase: HarvestContactsUseCase,
    private val setupSourcesUseCase: SetupSourcesUseCase
) : AndroidViewModel(application) {

    enum class SortOrder { NAME, STATUS, DATE }

    private val workManager = WorkManager.getInstance(application)

    private val _targets = repository.getAllTargets()
    
    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = kotlinx.coroutines.flow.MutableStateFlow(SortOrder.NAME)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _showIgnored = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showIgnored: StateFlow<Boolean> = _showIgnored

    private val prefs = application.getSharedPreferences("clean_underwear_prefs", Context.MODE_PRIVATE)
    private val _isOnboardingCompleted = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted

    val targets: StateFlow<List<Target>> = kotlinx.coroutines.flow.combine(
        _targets, _searchQuery, _sortOrder, _showIgnored
    ) { list, query, sort, showIgnored ->
        list.filter { 
            (showIgnored || it.status != TargetStatus.IGNORED) &&
            (it.displayName.contains(query, ignoreCase = true) || 
             it.phoneNumber.contains(query))
        }.sortedWith { a, b ->
            // Always prioritize status changes
            if (a.lastStatusChangeTimestamp != b.lastStatusChangeTimestamp) {
                b.lastStatusChangeTimestamp.compareTo(a.lastStatusChangeTimestamp)
            } else {
                when (sort) {
                    SortOrder.NAME -> a.displayName.compareTo(b.displayName)
                    SortOrder.STATUS -> a.status.name.compareTo(b.status.name)
                    SortOrder.DATE -> b.lastScrapedTimestamp.compareTo(a.lastScrapedTimestamp)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedSources = kotlinx.coroutines.flow.MutableStateFlow(setOf("Google", "Meta", "Apple", "Device"))
    val selectedSources: StateFlow<Set<String>> = _selectedSources

    fun toggleSource(source: String) {
        val current = _selectedSources.value.toMutableSet()
        if (current.contains(source)) {
            current.remove(source)
        } else {
            current.add(source)
        }
        _selectedSources.value = current
    }

    fun updateTarget(target: Target) {
        viewModelScope.launch {
            repository.updateTarget(target)
        }
    }

    fun ignoreTarget(target: Target) {
        viewModelScope.launch {
            repository.updateTarget(target.copy(status = TargetStatus.IGNORED))
        }
    }
    
    fun restoreTarget(target: Target) {
        viewModelScope.launch {
            repository.updateTarget(target.copy(status = TargetStatus.UNKNOWN))
        }
    }
    
    fun toggleShowIgnored() {
        _showIgnored.value = !_showIgnored.value
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun completeOnboarding() {
        prefs.edit {putBoolean("onboarding_done", true)}
        _isOnboardingCompleted.value = true
    }

    fun sweepContacts() {
        viewModelScope.launch {
            harvestContactsUseCase(selectedSources.value)
            setupSourcesUseCase()
        }
    }

    fun triggerManualInterrogation() {
        val request = OneTimeWorkRequestBuilder<ScrapingWorker>().build()
        workManager.enqueue(request)
    }

    fun scheduleDailyPanopticon() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ScrapingWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_panopticon",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }
}
