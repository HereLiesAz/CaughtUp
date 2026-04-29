package com.hereliesaz.caughtup.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test

class HtmlScraperTest {
    @Test
    fun scrapeMugshots_invalidUrl_returnsFalse() = runBlocking {
        val scraper = HtmlScraper()
        val result = scraper.scrapeMugshots("https://invalid.url.that.does.not.exist.com", "John Doe")
        assertFalse(result)
    }
}
