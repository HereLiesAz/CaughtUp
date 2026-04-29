package com.hereliesaz.caughtup.network

/**
 * The geography of grief and consequence.
 * Maps the arbitrary numbers assigned by telecom conglomerates to the specific URLs of municipal misery.
 */
class JurisdictionMapper {

    companion object {
        private val LA_REGEX = Regex("\\bla\\b")
    }

    private fun isLouisiana(residenceInfo: String?): Boolean {
        val lowerResidence = residenceInfo?.lowercase() ?: ""
        return lowerResidence.contains("new orleans") ||
               LA_REGEX.containsMatchIn(lowerResidence) ||
               lowerResidence.contains("louisiana")
    }

    fun getLockupUrl(areaCode: String, residenceInfo: String? = null): String? {
        val lowerResidence = residenceInfo?.lowercase() ?: ""

        if (isLouisiana(lowerResidence)) {
             if (areaCode == "504" || lowerResidence.contains("new orleans")) {
                 return "https://opso.us/docket/"
             }
             if (areaCode == "985" || lowerResidence.contains("st. tammany") || lowerResidence.contains("covington") || lowerResidence.contains("slidell")) {
                 return "https://www.stpso.com/inmate-roster/"
             }
             if (areaCode == "225" || lowerResidence.contains("baton rouge")) {
                 return "https://www.brla.gov/prison/"
             }
        }

        return when (areaCode) {
            "504" -> "https://opso.us/docket/"
            "985" -> "https://www.stpso.com/inmate-roster/" // Just in case they fled across the lake.
            "225" -> "https://www.brla.gov/prison/"
            else -> null // The abyss of the unknown.
        }
    }

    fun getObituaryUrl(areaCode: String, residenceInfo: String? = null): String? {
         val lowerResidence = residenceInfo?.lowercase() ?: ""

         if (isLouisiana(residenceInfo)) {
             if (areaCode == "504" || lowerResidence.contains("new orleans")) {
                 return "https://obits.nola.com/us/obituaries/nola/browse"
             }
         }

         return when (areaCode) {
            "504" -> "https://obits.nola.com/us/obituaries/nola/browse"
            else -> null
        }
    }
}

