package com.hereliesaz.cleanunderwear.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The cryptographer of mortality. 
 * Because human nomenclature is sloppy, and a shared first name shouldn't condemn a man to the digital grave.
 */
@Singleton
class IdentityVerifier @Inject constructor() {
    fun verifyIdentity(documentText: String, targetName: String): Boolean {
        val parts = targetName.split(" ").filter { it.isNotBlank() }
        
        // If they only gave us a mononym like "Madonna" or "Az", we fallback to basic brute force.
        if (parts.size < 2) return documentText.contains(targetName, ignoreCase = true)

        val firstName = parts.first()
        val lastName = parts.last()

        // Seek "First [Middle] Last" or "Last, First" amidst the municipal wreckage.
        val regex = Regex("(?i)($firstName\\s+(?:\\w+\\s+)?$lastName|$lastName,\\s+$firstName)")
        return regex.containsMatchIn(documentText)
    }
}

