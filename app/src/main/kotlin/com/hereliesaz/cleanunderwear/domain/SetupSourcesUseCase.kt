package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.network.OnDeviceResearchAgent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import kotlin.random.Random

/**
 * The initial interrogation. 
 * Locates the municipal rosters and obituary registries for all suspects in the ledger.
 */
class SetupSourcesUseCase @Inject constructor(
    private val repository: TargetRepository,
    private val researchAgent: OnDeviceResearchAgent
) {
    private val areaSemaphore = Semaphore(2) // Reduced concurrency to mitigate 429s

    suspend operator fun invoke(onProgress: (Float, String) -> Unit = { _, _ -> }) = coroutineScope {
        val targets = repository.getAllTargetSourceInfo()
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
                areaSemaphore.withPermit {
                    // Add a random jitter delay (2-5 seconds) before each area search to bypass bot detection
                    delay(Random.nextLong(2000, 5000))
                    
                    val areaTargets = targetsByArea[areaKey] ?: return@async
                    val first = areaTargets.first()
                    val areaDisplayName = first.residenceInfo ?: "Area Code ${first.areaCode ?: "Unknown"}"
                    
                    // 1. Arrest Source Search
                    onProgress(completedAreas.toFloat() / totalAreas, "Locating arrest roster for $areaDisplayName...")
                    val areaLockupUrl = researchAgent.getDynamicLockupUrl(first.areaCode, first.residenceInfo)
                    
                    delay(Random.nextLong(1000, 3000)) // Delay between different search types

                    // 2. Obituary Source Search
                    onProgress(completedAreas.toFloat() / totalAreas, "Locating obituary registry for $areaDisplayName...")
                    val areaObitUrl = researchAgent.getDynamicObituaryUrl(first.areaCode, first.residenceInfo)

                    // Apply these sources to EVERYONE in this area who is missing them
                    areaTargets.forEach { target ->
                        if (target.lockupUrl == null || target.obituaryUrl == null) {
                            repository.updateUrls(
                                id = target.id,
                                lockupUrl = target.lockupUrl ?: areaLockupUrl,
                                obituaryUrl = target.obituaryUrl ?: areaObitUrl
                            )
                        }
                    }
                    
                    synchronized(this@coroutineScope) {
                        completedAreas++
                    }
                    onProgress(completedAreas.toFloat() / totalAreas, "Sources established for $areaDisplayName.")
                }
            }
        }.awaitAll()
        
        onProgress(1f, "Intelligence sources linked by area.")
    }
}
