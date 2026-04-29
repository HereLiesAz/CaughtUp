package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.network.OnDeviceResearchAgent
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The initial interrogation. 
 * Locates the municipal rosters and obituary registries for all suspects in the ledger.
 */
class SetupSourcesUseCase @Inject constructor(
    private val repository: TargetRepository,
    private val researchAgent: OnDeviceResearchAgent
) {
    suspend operator fun invoke(onProgress: (Float, String) -> Unit = { _, _ -> }) {
        val targets = repository.getAllTargets().first()
        if (targets.isEmpty()) {
            onProgress(1f, "Registry is empty.")
            return
        }

        // Group targets by unique area (Area Code + Residence Info)
        val targetsByArea = targets.groupBy { "${it.areaCode ?: "000"}|${it.residenceInfo ?: ""}" }
        val uniqueAreas = targetsByArea.keys.toList()
        val totalAreas = uniqueAreas.size
        val totalTasks = totalAreas * 2
        var completedTasks = 0

        uniqueAreas.forEach { areaKey ->
            val areaTargets = targetsByArea[areaKey] ?: return@forEach
            val first = areaTargets.first()
            val areaDisplayName = first.residenceInfo ?: "Area Code ${first.areaCode ?: "Unknown"}"
            
            // 1. Arrest Source Search
            onProgress(completedTasks.toFloat() / totalTasks, "Gathering arrest sources for $areaDisplayName...")
            val areaLockupUrl = researchAgent.getDynamicLockupUrl(first.areaCode, first.residenceInfo)
            DiagnosticLogger.log("Arrest source for $areaDisplayName: $areaLockupUrl")
            completedTasks++

            // 2. Obituary Source Search
            onProgress(completedTasks.toFloat() / totalTasks, "Gathering obituary sources for $areaDisplayName...")
            val areaObitUrl = researchAgent.getDynamicObituaryUrl(first.areaCode, first.residenceInfo)
            DiagnosticLogger.log("Obituary source for $areaDisplayName: $areaObitUrl")
            completedTasks++

            // Apply these sources to EVERYONE in this area who is missing them
            areaTargets.forEach { target ->
                if (target.lockupUrl == null || target.obituaryUrl == null) {
                    repository.updateTarget(
                        target.copy(
                            lockupUrl = target.lockupUrl ?: areaLockupUrl,
                            obituaryUrl = target.obituaryUrl ?: areaObitUrl
                        )
                    )
                }
            }
        }
        
        onProgress(1f, "Intelligence sources linked by area.")
    }
}
