package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.network.OnDeviceResearchAgent
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
    suspend operator fun invoke() {
        val targets = repository.getAllTargets().first()
        
        targets.forEach { target ->
            // Only find sources if they are missing
            if (target.lockupUrl == null || target.obituaryUrl == null) {
                val lockupUrl = target.lockupUrl ?: researchAgent.getDynamicLockupUrl(target.areaCode, target.residenceInfo)
                val obitUrl = target.obituaryUrl ?: researchAgent.getDynamicObituaryUrl(target.areaCode, target.residenceInfo)
                
                repository.updateTarget(
                    target.copy(
                        lockupUrl = lockupUrl,
                        obituaryUrl = obitUrl
                    )
                )
            }
        }
    }
}
