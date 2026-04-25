package com.hereliesaz.caughtup.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.caughtup.CaughtUpApplication
import com.hereliesaz.caughtup.data.ContactHarvester
import com.hereliesaz.caughtup.data.Target
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The puppet master. Bridges the gap between the UI and the grim reality of the database.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as CaughtUpApplication).container.targetRepository
    private val harvester = ContactHarvester(application.contentResolver)

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
}
