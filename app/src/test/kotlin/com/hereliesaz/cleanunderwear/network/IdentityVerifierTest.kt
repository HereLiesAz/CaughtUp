package com.hereliesaz.cleanunderwear.network

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityVerifierTest {

    private val researchAgent: OnDeviceResearchAgent = mockk<OnDeviceResearchAgent>(relaxed = true).also {
        every { it.getNicknames(any<String>()) } returns emptyList()
    }
    private val verifier = IdentityVerifier(researchAgent)

    @Test
    fun verifyIdentity_exactMatch_returnsTrue() {
        val documentText = "John Doe was arrested on Tuesday."
        assertTrue(verifier.verifyIdentity(documentText, "John Doe").isMatch)
    }

    @Test
    fun verifyIdentity_lastFirstMatch_returnsTrue() {
        val documentText = "Doe, John"
        assertTrue(verifier.verifyIdentity(documentText, "John Doe").isMatch)
    }

    @Test
    fun verifyIdentity_middleNameMatch_returnsTrue() {
        val documentText = "John Robert Doe"
        assertTrue(verifier.verifyIdentity(documentText, "John Doe").isMatch)
    }

    @Test
    fun verifyIdentity_caseInsensitive_returnsTrue() {
        val documentText = "JOHN DOE"
        assertTrue(verifier.verifyIdentity(documentText, "John Doe").isMatch)
    }

    @Test
    fun verifyIdentity_noMatch_returnsFalse() {
        val documentText = "Jane Doe was arrested."
        assertFalse(verifier.verifyIdentity(documentText, "John Doe").isMatch)
    }

    @Test
    fun verifyIdentity_singleTokenName_skipsWithReason() {
        val result = verifier.verifyIdentity("B was here", "B")
        assertFalse("Single-letter name must never match", result.isMatch)
        assertTrue(result.skipped)
        assertEquals("mononym", result.skipReason)
    }

    @Test
    fun verifyIdentity_mononymAcrossDocument_neverMatches() {
        val documentText = "Madonna performed yesterday."
        val result = verifier.verifyIdentity(documentText, "Madonna")
        assertFalse(result.isMatch)
        assertTrue(result.skipped)
    }

    @Test
    fun verifyIdentity_shortToken_skips() {
        val result = verifier.verifyIdentity("Jo Smith was arrested", "Jo Smith")
        assertFalse(result.isMatch)
        assertTrue(result.skipped)
        assertEquals("first_token_unacceptable", result.skipReason)
    }

    @Test
    fun verifyIdentity_stopwordAsFirstName_skips() {
        val result = verifier.verifyIdentity(
            "Search results: Smith, John was booked.",
            "Search Smith"
        )
        assertFalse(result.isMatch)
        assertTrue(result.skipped)
    }

    @Test
    fun verifyIdentity_proximityRequired_distantTokensDoNotMatch() {
        val documentText = buildString {
            append("John ")
            repeat(50) { append("filler ") }
            append("Doe was the topic.")
        }
        val result = verifier.verifyIdentity(documentText, "John Doe")
        assertFalse("First and last 50 tokens apart must not match", result.isMatch)
    }

    @Test
    fun verifyIdentity_wordBoundary_singleLetterDoesNotMatchInsideOtherName() {
        // Confirms the underlying bug fix: "B Smith" must not match because "B"
        // alone is unverifiable and skipped before regex even runs. Also confirms
        // that even if a longer token were checked, \b boundaries prevent
        // partial-word matches like "B" -> "Bob".
        val result = verifier.verifyIdentity("Bob Smith and Jane Doe", "B Smith")
        assertFalse(result.isMatch)
        assertTrue(result.skipped)
    }

    @Test
    fun verifyIdentity_lastFirstWithStopword_skips() {
        val result = verifier.verifyIdentity("Doe, Inmate", "Inmate Doe")
        assertFalse(result.isMatch)
        assertTrue(result.skipped)
    }

    @Test
    fun verifyIdentity_hyphenatedName_matches() {
        val documentText = "Mary-Jane Watson reported missing."
        val result = verifier.verifyIdentity(documentText, "Mary-Jane Watson")
        assertTrue(result.isMatch)
    }
}
