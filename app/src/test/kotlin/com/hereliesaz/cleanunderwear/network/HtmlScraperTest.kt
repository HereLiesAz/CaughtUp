package com.hereliesaz.cleanunderwear.network

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Test

class HtmlScraperTest {
    @Test
    fun scrapeMugshots_invalidUrl_returnsNoMatch() = runBlocking {
        val okHttpClient = OkHttpClient()
        val verifier = mockk<IdentityVerifier>()
        val scraper = HtmlScraper(okHttpClient, verifier)
        val result = scraper.scrapeMugshots("https://invalid.url.that.does.not.exist.example", "John Doe")
        assertFalse(result.isMatch)
    }
}
