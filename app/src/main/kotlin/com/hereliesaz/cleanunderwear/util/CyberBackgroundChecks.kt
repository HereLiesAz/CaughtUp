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
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 10) return "$BASE_URL/phone"
        
        val formatted = "${digits.take(3)}-${digits.drop(3).take(3)}-${digits.takeLast(4)}"
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
