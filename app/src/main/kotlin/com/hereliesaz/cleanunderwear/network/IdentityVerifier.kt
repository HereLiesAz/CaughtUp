package com.hereliesaz.cleanunderwear.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether a target's name appears in a fetched roster/locator page.
 *
 * The matcher requires *both* a first and a last token to appear in proximity
 * with word-boundary anchors, so a contact named "B" never matches the letter
 * "B" inside "Bob" on some unrelated booking page.
 */
@Singleton
class IdentityVerifier @Inject constructor(
    private val researchAgent: OnDeviceResearchAgent
) {
    fun verifyIdentity(documentText: String, targetName: String): VerificationResult {
        return when (val v = NameValidator.validate(targetName)) {
            is NameValidator.Result.Skip -> VerificationResult(
                isMatch = false,
                snippet = null,
                skipped = true,
                skipReason = v.reason
            )
            is NameValidator.Result.Ok -> matchProximity(documentText, v.first, v.last)
        }
    }

    private fun matchProximity(
        documentText: String,
        firstName: String,
        lastName: String
    ): VerificationResult {
        val firstNameCandidates = (listOf(firstName) + researchAgent.getNicknames(firstName))
            .filter { NameValidator.isAcceptableToken(it) }
            .distinct()

        val lastQ = Regex.escape(lastName)
        for (firstAlt in firstNameCandidates) {
            val firstQ = Regex.escape(firstAlt)
            // "First [up to 3 intervening tokens] Last"
            val proximity = Regex("(?i)\\b$firstQ\\b(?:\\s+\\S+){0,3}\\s+\\b$lastQ\\b")
            // "Last, First"
            val inverted = Regex("(?i)\\b$lastQ\\b\\s*,\\s*\\b$firstQ\\b")

            val match = proximity.find(documentText) ?: inverted.find(documentText)
            if (match != null) {
                return VerificationResult(
                    isMatch = true,
                    snippet = extractSnippet(documentText, match.value, match.range.first)
                )
            }
        }
        return VerificationResult(isMatch = false, snippet = null)
    }

    private fun extractSnippet(fullText: String, match: String, matchIndex: Int = -1): String {
        val idx = if (matchIndex >= 0) matchIndex else fullText.indexOf(match, ignoreCase = true)
        if (idx == -1) return match

        val start = (idx - 100).coerceAtLeast(0)
        val end = (idx + match.length + 100).coerceAtMost(fullText.length)

        return "..." + fullText.substring(start, end).replace("\n", " ").trim() + "..."
    }

    data class VerificationResult(
        val isMatch: Boolean,
        val snippet: String?,
        val skipped: Boolean = false,
        val skipReason: String? = null
    ) {
        companion object {
            fun fetchFailed() = VerificationResult(isMatch = false, snippet = null)
        }
    }
}
