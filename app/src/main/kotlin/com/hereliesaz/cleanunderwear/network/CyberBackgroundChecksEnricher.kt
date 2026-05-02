package com.hereliesaz.cleanunderwear.network

import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.util.CyberBackgroundChecks
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phone- and name-based lookups against cyberbackgroundchecks.com.
 *
 * Strategy (in order, stopping at the first hit):
 *   1. If the target has a phone number, try /phone/<digits>.
 *   2. If a real name is known, try /people/<first>-<last>/<state> (state guessed from areaCode
 *      or residenceInfo) and parse the first result card.
 *
 * The site is JS-heavy and behind bot detection, so we drive everything through WebViewScraper
 * (the existing "ghost town" mode), which carries a real Chrome UA and lets Cloudflare's
 * JavaScript challenge complete before we read the DOM.
 */
@Singleton
class CyberBackgroundChecksEnricher @Inject constructor(
    private val scraper: WebViewScraper
) {
    /**
     * Returns a copy of [target] with whatever fields cybg surfaced, or null if the lookup found
     * nothing useful.
     */
    suspend fun enrich(target: Target): Target? {
        val byPhone = target.phoneNumber
            ?.filter { it.isDigit() }
            ?.takeIf { it.length >= 10 }
            ?.let { lookupByPhone(it) }
        if (byPhone != null) {
            DiagnosticLogger.log("CYBG: phone lookup populated ${target.phoneNumber} → ${byPhone.name ?: "<no name>"}")
            return mergeFindings(target, byPhone)
        }

        val byName = target.displayName
            .takeIf { it.isNotBlank() && it != "Unnamed Entity" && !it.startsWith("Unnamed Entity (") }
            ?.let { lookupByName(it, stateGuess(target)) }
        if (byName != null) {
            DiagnosticLogger.log("CYBG: name lookup populated ${target.displayName}")
            return mergeFindings(target, byName)
        }

        return null
    }

    private suspend fun lookupByPhone(digits: String): Findings? {
        val url = CyberBackgroundChecks.getPhoneSearchUrl(digits)
        val doc = scraper.scrapeGhostTown(url) ?: return null

        val name = doc.select(".name, h1.full-name, .person-name").firstOrNull()?.text()?.trim()
            ?.takeIf { it.isNotBlank() }
        val address = doc.select(".address, .current-address, .person-address").firstOrNull()
            ?.text()?.trim()?.takeIf { it.isNotBlank() }

        if (name == null && address == null) return null
        return Findings(name = name, address = address, phone = digits)
    }

    private suspend fun lookupByName(displayName: String, state: String?): Findings? {
        val parts = displayName.split(" ").filter { it.isNotBlank() }
        if (parts.size < 2) return null

        val baseUrl = CyberBackgroundChecks.getNameSearchUrl(displayName)
        val url = if (state.isNullOrBlank()) baseUrl
                  else "$baseUrl/${state.lowercase().replace(" ", "-")}"

        val doc = scraper.scrapeGhostTown(url) ?: return null

        // The first result card in the search list. CSS classes drift; we try a few candidates.
        val firstCard = doc.select(".person-card, .result-card, .search-result").firstOrNull()
            ?: doc

        val name = firstCard.select(".name, .full-name, h2, h3").firstOrNull()?.text()?.trim()
            ?.takeIf { it.isNotBlank() }
        val address = firstCard.select(".address, .current-address, .city-state").firstOrNull()
            ?.text()?.trim()?.takeIf { it.isNotBlank() }
        val phone = firstCard.select(".phone, .phone-number, [href^=tel]").firstOrNull()
            ?.text()?.trim()?.filter { it.isDigit() || it == '+' }?.takeIf { it.length >= 10 }

        if (name == null && address == null && phone == null) return null
        return Findings(name = name, address = address, phone = phone)
    }

    private fun stateGuess(target: Target): String? {
        // Prefer the residence info if it looks like "City, ST 12345"
        val residence = target.residenceInfo
        if (!residence.isNullOrBlank()) {
            val twoLetterState = Regex(",\\s*([A-Z]{2})\\b").find(residence)?.groupValues?.get(1)
            if (twoLetterState != null) return twoLetterState
            // Also accept "City, State Name 12345" -> grab the word(s) between commas
            val pieces = residence.split(",").map { it.trim() }
            if (pieces.size >= 2) return pieces[1].split(" ").firstOrNull()?.takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun mergeFindings(target: Target, f: Findings): Target {
        return target.copy(
            displayName = if (target.displayName == "Unnamed Entity" ||
                target.displayName.startsWith("Unnamed Entity (")) {
                f.name ?: target.displayName
            } else target.displayName,
            phoneNumber = target.phoneNumber ?: f.phone,
            residenceInfo = target.residenceInfo ?: f.address,
            areaCode = target.areaCode ?: f.phone?.takeLast(10)?.take(3)
        )
    }

    private data class Findings(val name: String?, val address: String?, val phone: String?)
}
