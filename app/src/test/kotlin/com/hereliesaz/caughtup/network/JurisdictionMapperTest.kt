package com.hereliesaz.caughtup.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JurisdictionMapperTest {

    private val mapper = JurisdictionMapper()

    @Test
    fun getLockupUrl_validAreaCode_returnsUrl() {
        assertEquals("https://opso.us/docket/", mapper.getLockupUrl("504"))
        assertEquals("https://www.stpso.com/inmate-roster/", mapper.getLockupUrl("985"))
        assertEquals("https://www.brla.gov/prison/", mapper.getLockupUrl("225"))
    }

    @Test
    fun getLockupUrl_invalidAreaCode_returnsNull() {
        assertNull(mapper.getLockupUrl("999"))
    }

    @Test
    fun getObituaryUrl_validAreaCode_returnsUrl() {
        assertEquals("https://obits.nola.com/us/obituaries/nola/browse", mapper.getObituaryUrl("504"))
    }

    @Test
    fun getObituaryUrl_invalidAreaCode_returnsNull() {
        assertNull(mapper.getObituaryUrl("985"))
        assertNull(mapper.getObituaryUrl("225"))
        assertNull(mapper.getObituaryUrl("999"))
    }
}
