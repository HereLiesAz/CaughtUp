package com.hereliesaz.cleanunderwear.util

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

        // CBC searches must NEVER include the leading "1" country code.
        // Loop because raw paste can yield "1 1 555..." → "115551234567".
        // Stops once digits drops to 10, so a real 10-digit number that
        // happens to start with "1" (e.g. "1112223333") is left alone.
        while (digits.length >= 11 && digits.startsWith("1")) {
            digits = digits.substring(1)
        }
        if (digits.length < 10) return "$BASE_URL/phone"

        // Anchor on the most-significant 10 digits (area code first), so any
        // trailing extension noise can't shift the area code out of view.
        val core = digits.take(10)
        val formatted = "${core.substring(0, 3)}-${core.substring(3, 6)}-${core.substring(6)}"
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
        // CBC requires the literal email — username@domain.com. Earlier code
        // mangled "@" → "-at-" and "." → "-", which CBC silently rejects.
        val normalized = email.trim().lowercase()
        if (!normalized.contains("@")) return "$BASE_URL/email"
        return "$BASE_URL/email/${encodeEmailPathSegment(normalized)}"
    }

    /**
     * Percent-encode an email for use in a URL path segment, leaving "@" and
     * "." literal (CBC needs to see those) and percent-encoding everything
     * outside the unreserved set. Pure JVM so unit tests don't need Robolectric.
     */
    private fun encodeEmailPathSegment(s: String): String {
        val out = StringBuilder(s.length)
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            val keep = (c in 'A'.code..'Z'.code) ||
                (c in 'a'.code..'z'.code) ||
                (c in '0'.code..'9'.code) ||
                c == '-'.code || c == '_'.code || c == '.'.code ||
                c == '~'.code || c == '@'.code
            if (keep) out.append(c.toChar())
            else out.append('%').append("%02X".format(c))
        }
        return out.toString()
    }
}
