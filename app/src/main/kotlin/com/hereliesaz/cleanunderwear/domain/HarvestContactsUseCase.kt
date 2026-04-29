package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.ContactHarvester
import com.hereliesaz.cleanunderwear.data.TargetRepository
import javax.inject.Inject

class HarvestContactsUseCase @Inject constructor(
    private val harvester: ContactHarvester,
    private val repository: TargetRepository
) {
    suspend operator fun invoke(allowedSources: Set<String> = emptySet()) {
        val freshMeat = harvester.harvestContacts(allowedSources)
        repository.insertTargets(freshMeat)
    }
}
