package com.hereliesaz.cleanunderwear.network

/**
 * Decides whether a contact's name is safe to feed into a roster/locator search.
 *
 * Single-letter or single-token names ("B", "Madonna") and stopword-class tokens
 * ("Inmate", "Search") matched substring-wise against any roster page, producing
 * false-positive INCARCERATED hits. Anything that fails this validator is routed
 * to identity enrichment instead of being silently scraped.
 */
object NameValidator {
    private const val MIN_TOKEN_LEN = 3

    private val BAD_NAME_TOKENS: Set<String> = setOf(
        // English stopwords
        "the", "and", "for", "are", "but", "not", "you", "all", "any", "can",
        "had", "her", "was", "one", "our", "out", "day", "get", "has", "him",
        "his", "how", "man", "new", "now", "old", "see", "two", "way", "who",
        "boy", "did", "its", "let", "put", "say", "she", "too", "use",
        // Roster / locator page chrome
        "inmate", "name", "search", "booking", "release", "charge", "case",
        "court", "county", "sheriff", "page", "next", "prev", "results",
        "first", "last", "male", "female", "white", "black", "status",
        "offender", "subject", "race", "gender", "dob", "age", "bond",
        // App-internal placeholders
        "unknown", "unnamed", "entity"
    )

    sealed class Result {
        data class Ok(val first: String, val last: String) : Result()
        data class Skip(val reason: String) : Result()
    }

    /**
     * Splits raw display text into name tokens, stripping punctuation other than
     * hyphens and apostrophes (which legitimately appear in names).
     */
    fun tokenize(rawName: String): List<String> {
        return rawName
            .split(Regex("\\s+"))
            .map { token -> token.filter { it.isLetter() || it == '-' || it == '\'' } }
            .filter { it.isNotEmpty() }
    }

    /**
     * Validates a tokenized name. Returns [Result.Ok] with the (first, last) pair
     * if both tokens pass, or [Result.Skip] with a reason describing which rule
     * failed.
     */
    fun validate(tokens: List<String>): Result {
        if (tokens.size < 2) return Result.Skip("mononym")
        val first = tokens.first()
        val last = tokens.last()
        if (!isAcceptableToken(first)) return Result.Skip("first_token_unacceptable")
        if (!isAcceptableToken(last)) return Result.Skip("last_token_unacceptable")
        return Result.Ok(first, last)
    }

    fun validate(rawName: String): Result = validate(tokenize(rawName))

    /**
     * A token is acceptable as a name part if it is at least [MIN_TOKEN_LEN]
     * characters and not in the [BAD_NAME_TOKENS] reject list.
     */
    fun isAcceptableToken(token: String): Boolean {
        if (token.length < MIN_TOKEN_LEN) return false
        if (BAD_NAME_TOKENS.contains(token.lowercase())) return false
        return true
    }

    fun isVerifiable(rawName: String): Boolean = validate(rawName) is Result.Ok
}
