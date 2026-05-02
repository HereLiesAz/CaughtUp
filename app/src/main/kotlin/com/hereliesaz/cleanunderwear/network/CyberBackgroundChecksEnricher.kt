package com.hereliesaz.cleanunderwear.network

import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.domain.BrowserMission
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identity enrichment via cyberbackgroundchecks.com.
 *
 * The site is JS-heavy and bot-defended; the user's real browser session is
 * the only reliable way to reach the result page. So this class does *not*
 * fetch anything — it picks the best [BrowserMission] for a target, and
 * parses the HTML the user-visible [com.hereliesaz.cleanunderwear.ui.BrowserScreen]
 * brings back.
 *
 * Search-mode priority (per the user, "if there's a contact, then at least
 * ONE of those ways is searchable"):
 *
 *   phone → email → address → name
 */
@Singleton
class CyberBackgroundChecksEnricher @Inject constructor() {

    /**
     * Returns the most-precise mission this target supports, or null if none
     * of the four search inputs are present (caller should mark the target
     * ENRICHMENT_FAILED).
     */
    fun pickMission(target: Target): BrowserMission? {
        val phone = target.phoneNumber
            ?.filter { it.isDigit() }
            ?.takeIf { it.length >= 10 }
        if (phone != null) return BrowserMission.CbcByPhone(phone)

        val email = target.email?.takeIf { it.isNotBlank() && it.contains("@") }
        if (email != null) return BrowserMission.CbcByEmail(email)

        val address = target.residenceInfo?.takeIf { it.isNotBlank() }
        if (address != null) return BrowserMission.CbcByAddress(address)

        val displayName = target.displayName.takeIf {
            it.isNotBlank() &&
                it != "Unnamed Entity" &&
                !it.startsWith("Unnamed Entity (") &&
                it.split(" ").size >= 2
        }
        if (displayName != null) return BrowserMission.CbcByName(displayName)

        return null
    }

    /**
     * Pulls name / address / phone out of a CBC results page. Returns null
     * if the page didn't surface anything useful (no result, captcha wall,
     * etc.).
     */
    fun parseFindings(html: String): Findings? {
        if (html.isBlank()) return null
        val doc = Jsoup.parse(html)

        // The first result card. CSS classes drift across CBC redesigns,
        // so try a handful of plausible roots.
        val firstCard: Element = doc.selectFirst(
            ".person-card, .result-card, .search-result, .card-person, [data-result-card]"
        ) ?: doc

        val name = firstCard
            .selectFirst(".name, h1.full-name, .full-name, .person-name, h2, h3")
            ?.text()?.trim()?.takeIf { it.isNotBlank() }

        val address = firstCard
            .selectFirst(".address, .current-address, .person-address, .city-state")
            ?.text()?.trim()?.takeIf { it.isNotBlank() }

        val phone = firstCard
            .selectFirst(".phone, .phone-number, [href^=tel]")
            ?.text()?.trim()
            ?.filter { it.isDigit() || it == '+' }
            ?.takeIf { it.length >= 10 }

        if (name == null && address == null && phone == null) return null
        return Findings(name = name, address = address, phone = phone)
    }

    /**
     * Folds [findings] into [target] without trampling fields the registry
     * already knows. Existing values win — enrichment fills gaps, doesn't
     * overwrite user-edited data.
     */
    fun merge(target: Target, findings: Findings): Target {
        val nameWasPlaceholder = target.displayName.isBlank() ||
            target.displayName == "Unnamed Entity" ||
            target.displayName.startsWith("Unnamed Entity (")

        val merged = target.copy(
            displayName = if (nameWasPlaceholder && findings.name != null) findings.name else target.displayName,
            phoneNumber = target.phoneNumber ?: findings.phone,
            residenceInfo = target.residenceInfo ?: findings.address,
            areaCode = target.areaCode ?: findings.phone?.takeLast(10)?.take(3)
        )
        DiagnosticLogger.log("CYBG merge: ${target.displayName} → ${merged.displayName} (phone=${merged.phoneNumber != null}, addr=${merged.residenceInfo != null})")
        return merged
    }

    data class Findings(val name: String?, val address: String?, val phone: String?)
}
