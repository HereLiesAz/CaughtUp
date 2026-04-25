package com.hereliesaz.caughtup.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * The digital buzzard. Descends upon archaic municipal HTML tables and funeral home DOMs 
 * to scavenge for the names of your associates.
 */
class HtmlScraper {
    
    suspend fun scrapeMugshots(url: String, targetName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect(url)
                // Disguising our automated dragnet as a bored human on a desktop.
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
            
            // A naive heuristic for an unpredictable reality. 
            // Assumes the local sheriff bothered to put the text in the DOM and not an image canvas.
            document.text().contains(targetName, ignoreCase = true)
        } catch (e: Exception) {
            Log.e("HtmlScraper", "Failed to breach the digital perimeter of $url", e)
            false
        }
    }
}
