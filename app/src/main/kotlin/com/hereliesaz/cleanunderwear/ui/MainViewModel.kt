package com.hereliesaz.cleanunderwear.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.data.TargetLite
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.data.TargetStatus
import kotlinx.coroutines.flow.Flow
import com.hereliesaz.cleanunderwear.domain.BrowserMission
import com.hereliesaz.cleanunderwear.domain.DeduplicateTargetsUseCase
import com.hereliesaz.cleanunderwear.domain.HarvestContactsUseCase
import com.hereliesaz.cleanunderwear.domain.PipelineCoordinator
import com.hereliesaz.cleanunderwear.domain.TriageTargetsUseCase
import kotlinx.coroutines.CompletableDeferred
import com.hereliesaz.cleanunderwear.worker.ScrapingWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val deduplicateTargetsUseCase: DeduplicateTargetsUseCase,
    private val triageTargetsUseCase: TriageTargetsUseCase,
    private val fbHarvester: com.hereliesaz.cleanunderwear.data.FacebookHarvester,
    private val whatsAppHarvester: com.hereliesaz.cleanunderwear.data.WhatsAppHarvester,
    private val instagramHarvester: com.hereliesaz.cleanunderwear.data.InstagramHarvester,
    private val googleContactsHarvester: com.hereliesaz.cleanunderwear.data.GoogleContactsHarvester,
    private val researchAgent: com.hereliesaz.cleanunderwear.network.OnDeviceResearchAgent,
    val sourceCatalog: com.hereliesaz.cleanunderwear.network.SourceCatalog,
    private val cbcEnricher: com.hereliesaz.cleanunderwear.network.CyberBackgroundChecksEnricher,
    private val pipelineCoordinator: PipelineCoordinator
) : AndroidViewModel(application) {

    enum class SortOrder { NAME, STATUS, DATE }

    private val workManager = WorkManager.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOrder = MutableStateFlow(SortOrder.NAME)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _showIgnored = MutableStateFlow(false)
    val showIgnored: StateFlow<Boolean> = _showIgnored

    private val _showNamelessFilter = MutableStateFlow<Boolean?>(null)
    val showNamelessFilter: StateFlow<Boolean?> = _showNamelessFilter

    private val _showEmailOnlyFilter = MutableStateFlow<Boolean?>(null)
    val showEmailOnlyFilter: StateFlow<Boolean?> = _showEmailOnlyFilter

    private val _hasEmailFilter = MutableStateFlow<Boolean?>(null)
    val hasEmailFilter: StateFlow<Boolean?> = _hasEmailFilter

    private val _hasAddressFilter = MutableStateFlow<Boolean?>(null)
    val hasAddressFilter: StateFlow<Boolean?> = _hasAddressFilter

    private val prefs = application.getSharedPreferences("clean_underwear_prefs", Context.MODE_PRIVATE)
    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted

    private val _googleFilter = MutableStateFlow<Boolean?>(null)
    val googleFilter: StateFlow<Boolean?> = _googleFilter

    private val _metaFilter = MutableStateFlow<Boolean?>(null)
    val metaFilter: StateFlow<Boolean?> = _metaFilter

    private val _appleFilter = MutableStateFlow<Boolean?>(null)
    val appleFilter: StateFlow<Boolean?> = _appleFilter

    private val _deviceFilter = MutableStateFlow<Boolean?>(null)
    val deviceFilter: StateFlow<Boolean?> = _deviceFilter

    /**
     * null = show all; true = only NEEDS_ENRICHMENT/ENRICHMENT_FAILED; false = only READY.
     */
    private val _pendingEnrichmentFilter = MutableStateFlow<Boolean?>(null)
    val pendingEnrichmentFilter: StateFlow<Boolean?> = _pendingEnrichmentFilter

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme
    
    private val _showDiagnosticLog = MutableStateFlow(prefs.getBoolean("show_diagnostic_log", false))
    val showDiagnosticLog: StateFlow<Boolean> = _showDiagnosticLog

    val diagnosticLogs = com.hereliesaz.cleanunderwear.util.DiagnosticLogger.logs

    private val _globalTargetLimit = MutableStateFlow(prefs.getInt("global_limit", 100))
    val globalTargetLimit: StateFlow<Int> = _globalTargetLimit

    private val _showManualEntryDialog = MutableStateFlow(false)
    val showManualEntryDialog: StateFlow<Boolean> = _showManualEntryDialog

    /**
     * The browser mission currently being shown to the user, if any. When
     * non-null, the UI overlays [com.hereliesaz.cleanunderwear.ui.BrowserScreen]
     * over the rest of the app. The result handler is captured so the caller
     * (e.g. the "Resolve Identity Now" button on TargetDetailScreen) can
     * react to whatever HTML the BrowserScreen extracts.
     */
    data class ActiveMission(
        val mission: com.hereliesaz.cleanunderwear.domain.BrowserMission,
        val onResult: (String?) -> Unit
    )

    private val _activeMission = MutableStateFlow<ActiveMission?>(null)
    val activeMission: StateFlow<ActiveMission?> = _activeMission

    fun launchMission(
        mission: com.hereliesaz.cleanunderwear.domain.BrowserMission,
        onResult: (String?) -> Unit = {}
    ) {
        _activeMission.value = ActiveMission(mission, onResult)
    }

    fun completeMission(html: String?) {
        val active = _activeMission.value
        _activeMission.value = null
        active?.onResult?.invoke(html)
    }

    fun cancelMission() {
        completeMission(null)
    }

    data class OperationState(
        val isRunning: Boolean = false,
        val description: String = "",
        val progress: Float = 0f // 0.0 to 1.0, or -1f for indeterminate
    )

    private val _operationState = MutableStateFlow(OperationState())
    val operationState: StateFlow<OperationState> = _operationState

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val targets: Flow<PagingData<TargetLite>> = combine(
        _searchQuery, _sortOrder, _showIgnored,
        _showNamelessFilter, _showEmailOnlyFilter, _hasEmailFilter, _hasAddressFilter,
        _googleFilter, _metaFilter, _appleFilter, _deviceFilter,
        _pendingEnrichmentFilter
    ) { args: Array<Any?> ->
        args
    }.flatMapLatest { args ->
        Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = true),
            pagingSourceFactory = {
                repository.searchTargets(
                    query = args[0] as String,
                    sort = (args[1] as SortOrder).name,
                    showIgnored = args[2] as Boolean,
                    namelessF = args[3] as Boolean?,
                    emailOnlyF = args[4] as Boolean?,
                    hasEmailF = args[5] as Boolean?,
                    hasAddressF = args[6] as Boolean?,
                    googleF = args[7] as Boolean?,
                    metaF = args[8] as Boolean?,
                    appleF = args[9] as Boolean?,
                    deviceF = args[10] as Boolean?,
                    pendingEnrichF = args[11] as Boolean?
                )
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun updateTarget(target: Target) {
        viewModelScope.launch {
            repository.updateTarget(target)
        }
    }

    fun ignoreTarget(id: Int) {
        viewModelScope.launch {
            repository.updateStatusOnly(id, TargetStatus.IGNORED, System.currentTimeMillis())
        }
    }

    fun restoreTarget(id: Int) {
        viewModelScope.launch {
            repository.updateStatusOnly(id, TargetStatus.UNKNOWN, System.currentTimeMillis())
        }
    }

    /**
     * Reactive single-target stream for the detail screen. Avoids materializing the full list.
     */
    fun observeTarget(id: Int): Flow<Target?> = repository.observeTargetById(id)
    
    fun toggleShowIgnored() {
        _showIgnored.value = !_showIgnored.value
    }

    fun setNamelessFilter(filter: Boolean?) {
        _showNamelessFilter.value = filter
    }

    fun setEmailOnlyFilter(filter: Boolean?) {
        _showEmailOnlyFilter.value = filter
    }

    fun setEmailFilter(hasEmail: Boolean?) {
        _hasEmailFilter.value = hasEmail
    }

    fun setAddressFilter(hasAddress: Boolean?) {
        _hasAddressFilter.value = hasAddress
    }

    fun setGoogleFilter(filter: Boolean?) { _googleFilter.value = filter }
    fun setMetaFilter(filter: Boolean?) { _metaFilter.value = filter }
    fun setAppleFilter(filter: Boolean?) { _appleFilter.value = filter }
    fun setDeviceFilter(filter: Boolean?) { _deviceFilter.value = filter }
    fun setPendingEnrichmentFilter(filter: Boolean?) { _pendingEnrichmentFilter.value = filter }

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

    fun setDarkTheme(enabled: Boolean) {
        prefs.edit { putBoolean("dark_theme", enabled) }
        _isDarkTheme.value = enabled
    }

    fun setShowDiagnosticLog(enabled: Boolean) {
        prefs.edit { putBoolean("show_diagnostic_log", enabled) }
        _showDiagnosticLog.value = enabled
    }

    fun setGlobalTargetLimit(limit: Int) {
        prefs.edit { putInt("global_limit", limit) }
        _globalTargetLimit.value = limit
    }

    fun setShowManualEntryDialog(show: Boolean) {
        _showManualEntryDialog.value = show
    }

    fun addManualTarget(name: String, phone: String?, email: String?) {
        viewModelScope.launch {
            _operationState.value = OperationState(isRunning = true, description = "Processing manual ingestion...", progress = -1f)
            
            // Re-use de-duplication and AI validation logic via UseCase if possible, 
            // or just build a single Target and insert.
            val target = Target(
                displayName = name,
                phoneNumber = phone,
                email = email,
                areaCode = phone?.filter { it.isDigit() }?.let { if (it.length >= 10) it.takeLast(10).take(3) else "LOCAL" },
                sourceAccount = "Manual Entry",
                status = TargetStatus.UNKNOWN
            )
            
            // We pass it through the Harvester's processor by wrapping in a list
            harvestContactsUseCase.processManualTargets(listOf(target))
            
            _operationState.value = OperationState(isRunning = false)
        }
    }

    fun sweepContacts() {
        val activeSources = mutableSetOf<String>()
        if (_googleFilter.value != false) activeSources.add("Google")
        if (_metaFilter.value != false) activeSources.add("Meta")
        if (_appleFilter.value != false) activeSources.add("Apple")
        if (_deviceFilter.value != false) activeSources.add("Device")

        Log.d("MainViewModel", "Sweeping contacts with sources: $activeSources")
        viewModelScope.launch {
            _operationState.value = OperationState(isRunning = true, description = "Scything local databases...", progress = 0.1f)
            
            // 1. Ingest raw data from enabled sources
            harvestContactsUseCase(activeSources)
            
            // 2. Consolidate and deduplicate the full registry
            _operationState.value = _operationState.value.copy(description = "Merging duplicate identities...", progress = 0.4f)
            deduplicateTargetsUseCase { sub, desc -> }
            
            // 3. Triage for monitorability (Parallelized)
            _operationState.value = _operationState.value.copy(description = "Triaging registry for vigil readiness...", progress = 0.7f)
            triageTargetsUseCase { sub, desc -> 
                _operationState.value = _operationState.value.copy(progress = 0.7f + (sub * 0.3f))
            }
            
            _operationState.value = OperationState(isRunning = false)
        }
    }

    fun harvestFacebook() {
        viewModelScope.launch {
            pipelineCoordinator.runExclusive("facebook-harvest") {
                _operationState.value = OperationState(
                    isRunning = true,
                    description = "Awaiting visible Facebook session...",
                    progress = -1f
                )
                val html = launchMissionAndAwait(BrowserMission.HarvestFacebookFriends)
                if (html.isNullOrBlank()) {
                    _operationState.value = OperationState(isRunning = false)
                    return@runExclusive
                }
                _operationState.value = _operationState.value.copy(
                    description = "Parsing Facebook friend list..."
                )
                val friends = fbHarvester.parseFriendsHtml(html)
                harvestContactsUseCase.processManualTargets(friends)
                _operationState.value = OperationState(isRunning = false)
            }
        }
    }

    /**
     * Walks every UNVERIFIED contact and resolves their identity through the
     * visible cyberbackgroundchecks browser flow, one mission at a time.
     *
     * Per the user's rule, this is *user-initiated*; the auto pipeline never
     * triggers it. If the auto pipeline (or any other coordinator-protected
     * job) is in flight, this batch queues until that finishes.
     */
    fun resolveUnverifiedBatch() {
        viewModelScope.launch {
            pipelineCoordinator.runExclusive("user-enrichment") {
                val unverified = repository
                    .getTargetWorkInfoPaged(limit = 5000, offset = 0)
                    .filter { it.status == TargetStatus.UNVERIFIED }

                if (unverified.isEmpty()) {
                    _operationState.value = OperationState(
                        isRunning = false,
                        description = "No UNVERIFIED contacts queued."
                    )
                    return@runExclusive
                }

                val total = unverified.size
                _operationState.value = OperationState(
                    isRunning = true,
                    description = "Resolving 1/$total via cyberbackgroundchecks...",
                    progress = 0f
                )

                for ((index, lite) in unverified.withIndex()) {
                    val target = repository.getTargetById(lite.id) ?: continue
                    val mission = cbcEnricher.pickMission(target) ?: run {
                        repository.updateMonitorabilityState(
                            target.id,
                            com.hereliesaz.cleanunderwear.data.MonitorabilityState.ENRICHMENT_FAILED
                        )
                        return@run null
                    } ?: continue

                    _operationState.value = OperationState(
                        isRunning = true,
                        description = "${index + 1}/$total · ${target.displayName} · ${mission.label}",
                        progress = index.toFloat() / total
                    )

                    val html = launchMissionAndAwait(mission)
                    if (html.isNullOrBlank()) {
                        // User cancelled this row — stop the whole batch
                        // rather than blow through every UNVERIFIED contact
                        // without their attention.
                        _operationState.value = OperationState(isRunning = false)
                        return@runExclusive
                    }

                    val findings = cbcEnricher.parseFindings(html)
                    if (findings != null) {
                        val merged = cbcEnricher.merge(target, findings).copy(
                            status = TargetStatus.MONITORING,
                            monitorabilityState =
                                com.hereliesaz.cleanunderwear.data.MonitorabilityState.READY
                        )
                        repository.updateTarget(merged)
                    } else {
                        repository.updateMonitorabilityState(
                            target.id,
                            com.hereliesaz.cleanunderwear.data.MonitorabilityState.ENRICHMENT_FAILED
                        )
                    }
                }

                _operationState.value = OperationState(
                    isRunning = false,
                    description = "Resolved $total UNVERIFIED contact${if (total == 1) "" else "s"}."
                )
            }
        }
    }

    /**
     * Launches a [BrowserMission] and suspends until BrowserScreen reports a
     * result (user tapped Run Automation) or cancels (user tapped back / Done).
     */
    private suspend fun launchMissionAndAwait(mission: BrowserMission): String? {
        val deferred = CompletableDeferred<String?>()
        launchMission(mission) { html -> deferred.complete(html) }
        return deferred.await()
    }

    fun harvestWhatsApp() {
        viewModelScope.launch {
            _operationState.value = OperationState(isRunning = true, description = "Linking to WhatsApp Web...", progress = -1f)
            val contacts = whatsAppHarvester.harvestContacts()
            harvestContactsUseCase.processManualTargets(contacts)
            _operationState.value = OperationState(isRunning = false)
        }
    }

    fun harvestInstagram() {
        viewModelScope.launch {
            _operationState.value = OperationState(isRunning = true, description = "Scything Instagram followers...", progress = -1f)
            val followers = instagramHarvester.harvestFollowers()
            harvestContactsUseCase.processManualTargets(followers)
            _operationState.value = OperationState(isRunning = false)
        }
    }

    fun harvestGoogleContacts() {
        viewModelScope.launch {
            _operationState.value = OperationState(isRunning = true, description = "Pulling Google Contacts roster...", progress = -1f)
            val contacts = googleContactsHarvester.harvestContacts()
            harvestContactsUseCase.processManualTargets(contacts)
            _operationState.value = OperationState(isRunning = false)
        }
    }

    fun triggerManualInterrogation() {
        val request = OneTimeWorkRequestBuilder<ScrapingWorker>().build()
        workManager.enqueue(request)
        
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        androidx.work.WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getFloat("progress", -1f)
                            val description = workInfo.progress.getString("description") ?: "Actively Interrogating Roster Targets..."
                            
                            _operationState.value = OperationState(
                                isRunning = true, 
                                description = description,
                                progress = progress
                            )
                        }
                        androidx.work.WorkInfo.State.SUCCEEDED, 
                        androidx.work.WorkInfo.State.FAILED, 
                        androidx.work.WorkInfo.State.CANCELLED -> {
                            _operationState.value = OperationState(isRunning = false)
                        }
                        else -> {}
                    }
                }
            }
        }
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
