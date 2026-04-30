package com.hereliesaz.cleanunderwear.data

import com.hereliesaz.cleanunderwear.network.WebViewScraper
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import com.hereliesaz.cleanunderwear.data.TargetStatus
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacebookHarvester @Inject constructor(
    private val scraper: WebViewScraper
) {
    private val INJECTION_SCRIPT = """
        (function() {
            let lastHeight = document.body.scrollHeight;
            let scrollInterval = setInterval(() => {
                window.scrollTo(0, document.body.scrollHeight);
                let newHeight = document.body.scrollHeight;
                if (newHeight === lastHeight) {
                    clearInterval(scrollInterval);
                    AndroidInterface.processHtml(document.documentElement.outerHTML);
                }
                lastHeight = newHeight;
            }, 2000);
        })();
    """.trimIndent()

    suspend fun harvestFriends(): List<Target> {
        val url = "https://mbasic.facebook.com/me/friends" // Using mbasic for easier parsing if possible, or standard
        val html = scraper.scrapeWithInjection("https://www.facebook.com/me/friends", INJECTION_SCRIPT) ?: return emptyList()
        
        return parseFriendsHtml(html)
    }

    private fun parseFriendsHtml(html: String): List<Target> {
        val doc = Jsoup.parse(html)
        val friends = mutableListOf<Target>()
        
        // Facebook's CSS classes change frequently, so we look for common patterns in the "Friends" list
        // Typically entries are inside anchor tags or divs with specific roles
        val elements = doc.select("a[href*='/user/'], a[href*='profile.php']")
        
        elements.forEach { element ->
            val name = element.text()
            if (name.isNotBlank() && !name.contains("Friends", ignoreCase = true)) {
                friends.add(Target(
                    displayName = name,
                    sourceAccount = "Meta (Facebook)",
                    status = TargetStatus.UNKNOWN
                ))
            }
        }
        
        DiagnosticLogger.log("Facebook Harvest: Identified ${friends.size} potential targets from social graph.")
        return friends.distinctBy { it.displayName }
    }
}
