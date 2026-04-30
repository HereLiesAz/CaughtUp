package com.hereliesaz.cleanunderwear.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject

/**
 * The digital buzzard. Descends upon archaic municipal HTML tables and funeral home DOMs 
 * to scavenge for the names of your associates.
 */
class HtmlScraper @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val verifier: IdentityVerifier
) {

    suspend fun scrapeMugshots(url: String, targetName: String): IdentityVerifier.VerificationResult = withContext(Dispatchers.IO) {
        try {
            val sanitizedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url

            val request = Request.Builder()
                .url(sanitizedUrl)
                // Disguising our automated dragnet as a bored human on a desktop.
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
 
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext IdentityVerifier.VerificationResult(false, null)
            
            val html = response.body?.string() ?: return@withContext IdentityVerifier.VerificationResult(false, null)
            val document = Jsoup.parse(html)
            
            verifier.verifyIdentity(document.text(), targetName)
        } catch (e: Exception) {
            Log.e("HtmlScraper", "Failed to breach the digital perimeter of $url", e)
            IdentityVerifier.VerificationResult(false, null)
        }
    }
}
