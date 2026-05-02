package com.hereliesaz.cleanunderwear.domain

import com.hereliesaz.cleanunderwear.data.Target
import com.hereliesaz.cleanunderwear.data.TargetLite
import com.hereliesaz.cleanunderwear.data.TargetRepository
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import javax.inject.Inject

/**
 * Walks the entire repository and merges duplicate Targets into one canonical record per
 * identity. Two Targets collide when they share a normalized phone number, an email, or
 * (when neither is present) a normalized display name.
 *
 * The first pass uses [TargetLite] so we never load the heavy verification-snippet/URL columns
 * for the bulk of rows that won't collide. Only collision groups get hydrated to full [Target]
 * rows for the merge.
 *
 * The "winning" record is whichever lite row has the most non-null intel fields (with a small
 * deference to the row with the freshest scrape); the loser's unique fields are merged into
 * the winner before the loser is deleted.
 */
class DeduplicateTargetsUseCase @Inject constructor(
    private val repository: TargetRepository
) {
    suspend operator fun invoke(onProgress: (Float, String) -> Unit = { _, _ -> }): Int {
        val groups = mutableMapOf<String, MutableList<Int>>() // identityKey -> list of IDs
        
        var offset = 0
        val chunkSize = 1000
        
        // 1. Identify collision candidates using memory-efficient chunking
        while (true) {
            val chunk = repository.getTargetsPaged(chunkSize, offset)
            if (chunk.isEmpty()) break
            
            for (t in chunk) {
                val key = identityKey(t)
                groups.getOrPut(key) { mutableListOf() }.add(t.id)
            }
            offset += chunkSize
            onProgress(0.1f, "Scanning registry for duplicates ($offset)...")
        }

        val collisions = groups.values.filter { it.size > 1 }
        if (collisions.isEmpty()) {
            onProgress(1f, "No duplicates found.")
            return 0
        }

        var processed = 0
        var merged = 0
        val totalSteps = collisions.size

        // 2. Resolve collisions (only hydrating full rows for actual duplicates)
        for (idGroup in collisions) {
            val fullGroup = repository.getTargetsByIds(idGroup)
            if (fullGroup.size < 2) continue
            
            val winner = pickWinnerFull(fullGroup)
            val losers = fullGroup.filter { it.id != winner.id }
            
            val mergedFull = losers.fold(winner) { acc, loser -> mergeFields(acc, loser) }
            repository.updateTarget(mergedFull)
            for (loser in losers) {
                repository.deleteTarget(loser)
                merged++
            }

            processed++
            onProgress(
                processed.toFloat() / totalSteps,
                "Merging duplicates: ${winner.displayName}"
            )
        }

        DiagnosticLogger.log("Dedup: collapsed $merged duplicate row(s) across $totalSteps identities")
        return merged
    }

    private fun pickWinnerFull(group: List<Target>): Target {
        return group.maxByOrNull { fullFieldScore(it) } ?: group.first()
    }

    private fun fullFieldScore(t: Target): Int {
        var score = 0
        if (t.displayName.isNotBlank() && t.displayName != "Unnamed Entity") score += 2
        if (!t.phoneNumber.isNullOrBlank()) score++
        if (!t.email.isNullOrBlank()) score++
        if (!t.residenceInfo.isNullOrBlank()) score++
        if (!t.areaCode.isNullOrBlank()) score++
        if (t.lastScrapedTimestamp > 0) score++
        if (t.lockupUrl != null) score++
        if (t.obituaryUrl != null) score++
        return score
    }

    private fun identityKey(t: TargetLite): String {
        val phone = t.phoneNumber?.filter { it.isDigit() }?.takeIf { it.length >= 7 }
        if (phone != null) return "phone:$phone"
        val email = t.email?.lowercase()?.trim()?.takeIf { it.isNotBlank() }
        if (email != null) return "email:$email"
        return "name:${t.displayName.lowercase().trim()}"
    }

    private fun mergeFields(into: Target, from: Target): Target {
        val mergedSources = mergeSources(into.sourceAccount, from.sourceAccount)
        return into.copy(
            displayName = preferNonPlaceholder(into.displayName, from.displayName),
            phoneNumber = into.phoneNumber ?: from.phoneNumber,
            email = into.email ?: from.email,
            areaCode = into.areaCode ?: from.areaCode,
            jurisdiction = into.jurisdiction ?: from.jurisdiction,
            residenceInfo = into.residenceInfo ?: from.residenceInfo,
            lockupUrl = into.lockupUrl ?: from.lockupUrl,
            obituaryUrl = into.obituaryUrl ?: from.obituaryUrl,
            sourceAccount = mergedSources,
            lastScrapedTimestamp = maxOf(into.lastScrapedTimestamp, from.lastScrapedTimestamp),
            lastStatusChangeTimestamp = maxOf(into.lastStatusChangeTimestamp, from.lastStatusChangeTimestamp),
            lastVerificationSnippet = into.lastVerificationSnippet ?: from.lastVerificationSnippet
        )
    }

    private fun preferNonPlaceholder(a: String, b: String): String {
        if (a == "Unnamed Entity" && b != "Unnamed Entity") return b
        if (a.startsWith("Unnamed Entity (") && !b.startsWith("Unnamed Entity (") && b != "Unnamed Entity") return b
        return a
    }

    private fun mergeSources(a: String?, b: String?): String? {
        val parts = mutableSetOf<String>()
        a?.split(", ")?.forEach { if (it.isNotBlank()) parts += it.trim() }
        b?.split(", ")?.forEach { if (it.isNotBlank()) parts += it.trim() }
        return if (parts.isEmpty()) null else parts.joinToString(", ")
    }
}
