package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.network.OnDeviceResearchAgent
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    suspend operator fun invoke(onProgress: (Float, String) -> Unit = { _, _ -> }) = coroutineScope {
        val targets = repository.getAllTargets().first()
        if (targets.isEmpty()) {
            onProgress(1f, "Registry is empty.")
            return@coroutineScope
        }

        // Group targets by unique area (Area Code + Residence Info)
        val targetsByArea = targets.groupBy { "${it.areaCode ?: "000"}|${it.residenceInfo ?: ""}" }
        val uniqueAreas = targetsByArea.keys.toList()
        val totalAreas = uniqueAreas.size
        
        var completedAreas = 0

        uniqueAreas.map { areaKey ->
            async {
                val areaTargets = targetsByArea[areaKey] ?: return@async
                val first = areaTargets.first()
                val areaDisplayName = first.residenceInfo ?: "Area Code ${first.areaCode ?: "Unknown"}"
                
                DiagnosticLogger.log("Scanning Area: $areaDisplayName")
                
                // 1. Arrest Source Search
                val areaLockupUrl = researchAgent.getDynamicLockupUrl(first.areaCode, first.residenceInfo)
                
                // 2. Obituary Source Search
                val areaObitUrl = researchAgent.getDynamicObituaryUrl(first.areaCode, first.residenceInfo)

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
                
                completedAreas++
                onProgress(completedAreas.toFloat() / totalAreas, "Linking sources for $areaDisplayName...")
            }
        }.awaitAll()
        
        onProgress(1f, "Intelligence sources linked by area.")
    }
}
