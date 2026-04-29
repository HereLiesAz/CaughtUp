package com.hereliesaz.cleanunderwear.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityVerifierTest {

    private val verifier = IdentityVerifier()

    @Test
    fun verifyIdentity_exactMatch_returnsTrue() {
        val documentText = "John Doe was arrested on Tuesday."
        assertTrue(verifier.verifyIdentity(documentText, "John Doe"))
    }

    @Test
    fun verifyIdentity_lastFirstMatch_returnsTrue() {
        val documentText = "Doe, John"
        assertTrue(verifier.verifyIdentity(documentText, "John Doe"))
    }

    @Test
    fun verifyIdentity_middleNameMatch_returnsTrue() {
        val documentText = "John Robert Doe"
        assertTrue(verifier.verifyIdentity(documentText, "John Doe"))
    }

    @Test
    fun verifyIdentity_caseInsensitive_returnsTrue() {
        val documentText = "JOHN DOE"
        assertTrue(verifier.verifyIdentity(documentText, "John Doe"))
    }

    @Test
    fun verifyIdentity_noMatch_returnsFalse() {
        val documentText = "Jane Doe was arrested."
        assertFalse(verifier.verifyIdentity(documentText, "John Doe"))
    }

    @Test
    fun verifyIdentity_mononymMatch_returnsTrue() {
        val documentText = "Madonna performed yesterday."
        assertTrue(verifier.verifyIdentity(documentText, "Madonna"))
    }

    @Test
    fun verifyIdentity_mononymNoMatch_returnsFalse() {
        val documentText = "Cher performed yesterday."
        assertFalse(verifier.verifyIdentity(documentText, "Madonna"))
    }
}
