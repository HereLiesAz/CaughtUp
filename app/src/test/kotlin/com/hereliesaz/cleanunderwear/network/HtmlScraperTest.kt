package com.hereliesaz.cleanunderwear.network

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Test

class HtmlScraperTest {
    @Test
    fun scrapeMugshots_invalidUrl_returnsFalse() = runBlocking {
        val okHttpClient = mockk<OkHttpClient>()
        val verifier = mockk<IdentityVerifier>()
        val scraper = HtmlScraper(okHttpClient, verifier)
        val result = scraper.scrapeMugshots("https://invalid.url.that.does.not.exist.com", "John Doe")
        assertFalse(result)
    }
}
