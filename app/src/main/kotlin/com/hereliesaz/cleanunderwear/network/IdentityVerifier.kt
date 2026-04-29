package com.hereliesaz.cleanunderwear.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The cryptographer of mortality. 
 * Because human nomenclature is sloppy, and a shared first name shouldn't condemn a man to the digital grave.
 */
@Singleton
class IdentityVerifier @Inject constructor(
    private val researchAgent: OnDeviceResearchAgent
) {
    fun verifyIdentity(documentText: String, targetName: String): VerificationResult {
        val parts = targetName.split(" ").filter { it.isNotBlank() }
        
        if (parts.size < 2) {
            val found = documentText.contains(targetName, ignoreCase = true)
            return VerificationResult(found, if (found) extractSnippet(documentText, targetName) else null)
        }

        val firstName = parts.first()
        val lastName = parts.last()
        val firstNames = listOf(firstName) + researchAgent.getNicknames(firstName)
        
        val variations = firstNames.flatMap { name ->
            listOf(
                "(?i)$name\\s+(?:\\w+\\s+)?$lastName",
                "(?i)$lastName,\\s+$name"
            )
        }
        
        val combinedRegex = variations.joinToString("|").toRegex()
        val match = combinedRegex.find(documentText)
        
        return if (match != null) {
            VerificationResult(true, extractSnippet(documentText, match.value, match.range.first))
        } else {
            VerificationResult(false, null)
        }
    }

    private fun extractSnippet(fullText: String, match: String, matchIndex: Int = -1): String {
        val idx = if (matchIndex >= 0) matchIndex else fullText.indexOf(match, ignoreCase = true)
        if (idx == -1) return match
        
        val start = (idx - 100).coerceAtLeast(0)
        val end = (idx + match.length + 100).coerceAtMost(fullText.length)
        
        return "..." + fullText.substring(start, end).replace("\n", " ").trim() + "..."
    }

    data class VerificationResult(val isMatch: Boolean, val snippet: String?)
}

