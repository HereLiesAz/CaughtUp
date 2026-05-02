package com.hereliesaz.cleanunderwear.data

import com.hereliesaz.cleanunderwear.network.WebViewScraper
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scrapes contacts.google.com for the operator's contact list.
 *
 * Requires an active Google session cookie in WebView storage (the user must have signed in to
 * a Google account in this WebView surface previously). When unauthenticated, contacts.google.com
 * redirects to accounts.google.com and parsing yields no rows — handled by returning empty.
 */
@Singleton
class GoogleContactsHarvester @Inject constructor(
    private val scraper: WebViewScraper
) {
    private val INJECTION_SCRIPT = """
        (function() {
            function rowsContainer() {
                return document.querySelector('main [role="grid"]')
                    || document.querySelector('[role="main"] [role="grid"]')
                    || document.querySelector('[role="grid"]');
            }
            let lastCount = -1;
            function step(attempts) {
                const grid = rowsContainer();
                if (!grid) {
                    if (attempts > 60) {
                        AndroidInterface.processHtml(document.documentElement.outerHTML);
                        return;
                    }
                    setTimeout(() => step(attempts + 1), 1000);
                    return;
                }
                window.scrollTo(0, document.body.scrollHeight);
                grid.scrollTop = grid.scrollHeight;
                const rows = grid.querySelectorAll('[role="row"]');
                if (rows.length === lastCount) {
                    AndroidInterface.processHtml(document.documentElement.outerHTML);
                    return;
                }
                lastCount = rows.length;
                setTimeout(() => step(0), 1500);
            }
            step(0);
        })();
    """.trimIndent()

    suspend fun harvestContacts(): List<Target> {
        val html = scraper.scrapeWithInjection(
            "https://contacts.google.com/",
            INJECTION_SCRIPT
        ) ?: return emptyList()
        return parseContactsHtml(Jsoup.parse(html))
    }

    private fun parseContactsHtml(doc: Document): List<Target> {
        val targets = mutableListOf<Target>()
        val phonePattern = Regex("[+]?[0-9][0-9 ()\\-]{6,}")

        val rows = doc.select("[role=row]")
        for (row in rows) {
            // Skip the header row.
            if (row.select("[role=columnheader]").isNotEmpty()) continue

            val cells = row.select("[role=gridcell]")
            if (cells.isEmpty()) continue

            val nameCell = cells.firstOrNull() ?: continue
            val displayName = nameCell.text().trim()
            if (displayName.isBlank()) continue

            val rowText = row.text()
            val emailMatch = Regex("[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}").find(rowText)?.value
            val phoneMatch = phonePattern.find(rowText)?.value?.filter {
                it.isDigit() || it == '+'
            }?.takeIf { it.length >= 7 }

            targets += Target(
                displayName = displayName,
                email = emailMatch,
                phoneNumber = phoneMatch,
                sourceAccount = "Google",
                status = TargetStatus.UNKNOWN
            )
        }

        val deduped = targets.distinctBy { (it.phoneNumber ?: it.email ?: it.displayName) }
        DiagnosticLogger.log("Google Contacts Harvest: Identified ${deduped.size} entries.")
        return deduped
    }
}
