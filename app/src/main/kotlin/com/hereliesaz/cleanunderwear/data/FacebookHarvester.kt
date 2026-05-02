package com.hereliesaz.cleanunderwear.data

import com.hereliesaz.cleanunderwear.network.WebViewScraper
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacebookHarvester @Inject constructor(
    private val scraper: WebViewScraper
) {
    /**
     * mbasic emits server-rendered HTML pages without React; the friend list paginates via "See More"
     * links. We click through pages by following the next cursor until it disappears, then dump.
     */
    private val INJECTION_SCRIPT = """
        (function() {
            async function followNext(maxPages) {
                for (let i = 0; i < maxPages; i++) {
                    const more = document.querySelector('a[href*="/friends/?"][href*="cursor"]')
                        || document.querySelector('a[href*="/friends?"][href*="cursor"]')
                        || Array.from(document.querySelectorAll('a')).find(a => /See more/i.test(a.textContent || ''));
                    if (!more) break;
                    const href = more.getAttribute('href');
                    if (!href) break;
                    try {
                        const resp = await fetch(href, { credentials: 'include' });
                        const html = await resp.text();
                        document.body.insertAdjacentHTML('beforeend', html);
                    } catch (e) { break; }
                }
                AndroidInterface.processHtml(document.documentElement.outerHTML);
            }
            followNext(20);
        })();
    """.trimIndent()

    suspend fun harvestFriends(): List<Target> {
        val html = scraper.scrapeWithInjection(
            "https://mbasic.facebook.com/me/friends",
            INJECTION_SCRIPT
        ) ?: return emptyList()
        return parseFriendsHtml(Jsoup.parse(html))
    }

    private fun parseFriendsHtml(doc: Document): List<Target> {
        val friends = mutableListOf<Target>()

        // mbasic friend rows are anchors whose href points at the user's profile and whose text
        // is the friend's display name. We exclude navigation chrome by ignoring known noise links.
        val noise = setOf(
            "Friends", "Mutual friends", "Followers", "Following",
            "See all", "See more", "Edit profile", "Home", "Menu"
        )
        val elements = doc.select("a[href]")

        for (element in elements) {
            val href = element.attr("href")
            val looksLikeProfile =
                href.contains("/profile.php?id=") ||
                href.matches(Regex("^/[A-Za-z0-9.\\-]+(\\?.*)?$")) ||
                href.contains("/friends/?profile_id=")
            if (!looksLikeProfile) continue

            val name = element.text().trim()
            if (name.isBlank() || name in noise) continue
            if (name.length < 2) continue

            friends += Target(
                displayName = name,
                sourceAccount = "Meta (Facebook)",
                status = TargetStatus.UNKNOWN
            )
        }

        val deduped = friends.distinctBy { it.displayName }
        DiagnosticLogger.log("Facebook Harvest: Identified ${deduped.size} potential targets from social graph.")
        return deduped
    }
}
