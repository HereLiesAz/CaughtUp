package com.hereliesaz.caughtup.network

/**
 * The geography of grief and consequence.
 * Maps the arbitrary numbers assigned by telecom conglomerates to the specific URLs of municipal misery.
 */
class JurisdictionMapper {

    fun getLockupUrl(areaCode: String): String? {
        return when (areaCode) {
            "504" -> "https://opso.us/docket/"
            "985" -> "https://www.stpso.com/inmate-roster/" // Just in case they fled across the lake.
            "225" -> "https://www.brla.gov/prison/"
            else -> null // The abyss of the unknown.
        }
    }

    fun getObituaryUrl(areaCode: String): String? {
         return when (areaCode) {
            "504" -> "https://obits.nola.com/us/obituaries/nola/browse"
            else -> null
        }
    }
}

