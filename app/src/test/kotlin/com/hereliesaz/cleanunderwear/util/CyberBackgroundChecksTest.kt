package com.hereliesaz.cleanunderwear.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CyberBackgroundChecksTest {

    @Test
    fun phone_tenDigits_formatsAsXxxXxxXxxx() {
        assertEquals(
            "https://www.cyberbackgroundchecks.com/phone/555-123-4567",
            CyberBackgroundChecks.getPhoneSearchUrl("(555) 123-4567")
        )
    }

    @Test
    fun phone_elevenDigitsWithLeadingOne_stripsCountryCode() {
        assertEquals(
            "https://www.cyberbackgroundchecks.com/phone/555-123-4567",
            CyberBackgroundChecks.getPhoneSearchUrl("+1 555-123-4567")
        )
    }

    @Test
    fun phone_thirteenDigitsWithExtension_stripsCountryCodeAndIgnoresExtension() {
        assertEquals(
            "https://www.cyberbackgroundchecks.com/phone/555-123-4567",
            CyberBackgroundChecks.getPhoneSearchUrl("1-555-123-4567 ext. 99")
        )
    }

    @Test
    fun phone_doubleFormattedElevenDigitsLikeOneOne_stripsLeadingOne() {
        // Raw paste oddities like "11 555 123 4567" reduce to digits "115551234567"
        // (length 12, starts with 1) — strip yields the proper 10-digit number.
        assertEquals(
            "https://www.cyberbackgroundchecks.com/phone/555-123-4567",
            CyberBackgroundChecks.getPhoneSearchUrl("1 1 555 123 4567")
        )
    }

    @Test
    fun phone_tooShort_fallsBackToBasePath() {
        assertEquals(
            "https://www.cyberbackgroundchecks.com/phone",
            CyberBackgroundChecks.getPhoneSearchUrl("555-123")
        )
    }

    @Test
    fun email_preservesAtAndDot_lowercases_andEncodesPlus() {
        assertEquals(
            "https://www.cyberbackgroundchecks.com/email/foo.bar%2Btag@example.com",
            CyberBackgroundChecks.getEmailSearchUrl("Foo.Bar+tag@Example.com")
        )
    }

    @Test
    fun email_trimsWhitespace() {
        assertEquals(
            "https://www.cyberbackgroundchecks.com/email/jane@example.com",
            CyberBackgroundChecks.getEmailSearchUrl("  Jane@example.com  ")
        )
    }

    @Test
    fun email_missingAtSign_fallsBackToBasePath() {
        assertEquals(
            "https://www.cyberbackgroundchecks.com/email",
            CyberBackgroundChecks.getEmailSearchUrl("noatsign")
        )
    }
}
