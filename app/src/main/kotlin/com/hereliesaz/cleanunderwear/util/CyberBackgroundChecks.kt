package com.hereliesaz.cleanunderwear.util

import java.net.URLEncoder

object CyberBackgroundChecks {
    private const val BASE_URL = "https://www.cyberbackgroundchecks.com"

    fun getNameSearchUrl(name: String): String {
        val parts = name.split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return "$BASE_URL/name"
        
        val slug = parts.joinToString("-").lowercase()
        return "$BASE_URL/name/$slug"
    }

    fun getPhoneSearchUrl(phone: String): String {
        var digits = phone.filter { it.isDigit() }
        
        // The country code 1 must be excluded for CBC phone searches.
        if (digits.length == 11 && digits.startsWith("1")) {
            digits = digits.substring(1)
        }

        if (digits.length < 10) return "$BASE_URL/phone"
        
        // CBC expects 10 digits in XXX-XXX-XXXX format.
        val last10 = digits.takeLast(10)
        val formatted = "${last10.substring(0, 3)}-${last10.substring(3, 6)}-${last10.substring(6)}"
        return "$BASE_URL/phone/$formatted"
    }

    fun getAddressSearchUrl(address: String): String {
        // Expected format: "123 Main St, City, State Zip"
        val parts = address.split(",").map { it.trim() }
        if (parts.isEmpty()) return "$BASE_URL/address"
        
        val streetSlug = parts[0].replace(" ", "-").lowercase()
        val citySlug = parts.getOrNull(1)?.replace(" ", "-")?.lowercase() ?: ""
        val statePart = parts.getOrNull(2)?.split(" ")?.firstOrNull()?.lowercase() ?: ""
        
        return if (citySlug.isNotEmpty() && statePart.isNotEmpty()) {
            "$BASE_URL/address/$streetSlug/$citySlug/$statePart"
        } else {
            "$BASE_URL/address/$streetSlug"
        }
    }

    fun getEmailSearchUrl(email: String): String {
        val slug = email.lowercase().replace("@", "-at-").replace(".", "-")
        return "$BASE_URL/email/$slug"
    }
}
