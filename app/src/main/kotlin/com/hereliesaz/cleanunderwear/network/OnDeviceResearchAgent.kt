package com.hereliesaz.cleanunderwear.network

import android.content.Context
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.flex.FlexDelegate
import java.io.FileInputStream
import java.net.URLEncoder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnDeviceResearchAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scraper: WebViewScraper
) {
    companion object {
        init {
            try {
                System.loadLibrary("tensorflowlite_flex_jni")
            } catch (e: Throwable) {
                // Native flex library loading handled by delegate if this fails
            }
        }
    }

    private var interpreter: Interpreter? = null
    private var triggers: Map<String, TriggerState> = emptyMap()
    private var nicknames: Map<String, List<String>> = emptyMap()

    data class TriggerState(
        val area_codes: List<String>,
        val priority_sources: List<String>
    )

    init {
        try {
            val modelBuffer = loadModelFile("research_agent.tflite")
            
            // Explicitly add FlexDelegate to support complex ops like FlexStringLower.
            // Using org.tensorflow version 2.16.1 for guaranteed compatibility.
            val options = Interpreter.Options().apply {
                addDelegate(FlexDelegate())
            }
            
            interpreter = Interpreter(modelBuffer, options)
            DiagnosticLogger.log("Intelligence Agent: Model loaded successfully with Flex support.")
        } catch (e: Exception) {
            DiagnosticLogger.log("Intelligence Agent Error: Failed to load LiteRT model. ${e.message}", DiagnosticLogger.LogEntry.LogLevel.ERROR)
            interpreter = null
        }
        loadAssets()
    }

    private fun loadAssets() {
        try {
            val gson = Gson()
            
            // Load Triggers
            val triggerJson = context.assets.open("scraper_triggers.json").bufferedReader().use { it.readText() }
            val triggerType = object : TypeToken<Map<String, TriggerState>>() {}.type
            triggers = gson.fromJson(triggerJson, triggerType)

            // Load Nicknames
            val nicknameJson = context.assets.open("nicknames.json").bufferedReader().use { it.readText() }
            val nicknameType = object : TypeToken<Map<String, List<String>>>() {}.type
            nicknames = gson.fromJson(nicknameJson, nicknameType)
        } catch (e: Exception) {
            // Fallback to empty if assets are missing
        }
    }

    fun getNicknames(name: String): List<String> {
        return nicknames[name.lowercase()] ?: emptyList()
    }

    /**
     * Uses the LiteRT model to determine if the provided text is likely a human name.
     */
    fun validatePersonName(text: String): Boolean {
        if (text.isBlank() || text == "Unnamed Entity") return false
        
        // Secondary Heuristic: A real name usually has at least one space and no numbers
        // Or if it's a single word, it should be alphabetic and longer than 1 char (e.g. "Az")
        val hasSpace = text.trim().contains(" ")
        val isAlphaOnly = text.all { it.isLetter() }
        val hasNoDigits = text.none { it.isDigit() }
        val looksLikeName = (hasSpace || (text.length > 1 && isAlphaOnly)) && hasNoDigits

        val score = scoreWithModel(text)
        
        val serviceKeywords = listOf("customer", "service", "support", "help", "bank", "office", "pizza", "taxi", "delivery", "store")
        val isService = serviceKeywords.any { text.contains(it, ignoreCase = true) }
        
        // If AI gives 0.0, we rely on the looksLikeName heuristic to avoid total failure
        val isValid = if (score == 0.0f) looksLikeName else (score > 0.3f && !isService)

        if (!isValid) {
            DiagnosticLogger.log("AI Flag: '$text' interrogated and rejected. (Score: $score, Heuristic: $looksLikeName)")
        }
        
        return isValid
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }


    suspend fun getDynamicLockupUrl(areaCode: String?, residenceInfo: String? = null): String {
        val safeAreaCode = areaCode ?: "000"
        val locationQuery = residenceInfo?.takeIf { it.isNotBlank() } ?: "Area Code $safeAreaCode"
        val stateTriggers = triggers.values.find { it.area_codes.contains(safeAreaCode) }
        
        val siteConstraint = stateTriggers?.priority_sources?.joinToString(" OR ") { "site:$it" } ?: ""
        val query = if (siteConstraint.isNotBlank()) {
            "($siteConstraint) \"inmate roster\" OR \"arrest log\" \"$locationQuery\""
        } else {
            "\"inmate roster\" OR \"arrest log\" \"$locationQuery\""
        }
        
        val fallback = stateTriggers?.priority_sources?.firstOrNull { it.contains(".gov") || it.contains(".us") }
            ?: "https://www.google.com/search?q=inmate+roster+$safeAreaCode"
            
        return executeAiSearch(query, fallback)
    }

    suspend fun getDynamicObituaryUrl(areaCode: String?, residenceInfo: String? = null): String {
        val safeAreaCode = areaCode ?: "000"
        val locationQuery = residenceInfo?.takeIf { it.isNotBlank() } ?: "Area Code $safeAreaCode"
        val stateTriggers = triggers.values.find { it.area_codes.contains(safeAreaCode) }
        
        val query = "obituary \"$locationQuery\""
        val fallback = stateTriggers?.priority_sources?.find { it.contains("obituaries") } 
            ?: "https://www.legacy.com/obituaries"
        
        return executeAiSearch(query, fallback)
    }

    private suspend fun executeAiSearch(query: String, fallbackUrl: String): String {
        // 1. Fetch Search Results
        val searchUrl = "https://html.duckduckgo.com/html/?q=${URLEncoder.encode(query, "UTF-8")}"
        val document = scraper.scrapeGhostTown(searchUrl) ?: return fallbackUrl

        // 2. Extract links and snippets
        val results = document.select(".result__body")
        val candidates = mutableListOf<Pair<String, String>>() // (URL, CombinedText)

        for (result in results) {
            val aTag = result.selectFirst(".result__title a")
            val snippet = result.selectFirst(".result__snippet")?.text() ?: ""
            val title = aTag?.text() ?: ""
            val link = aTag?.attr("href") ?: continue
            
            // Basic clean up of duckduckgo redirect urls if any
            val cleanLink = if (link.startsWith("//duckduckgo.com/l/?uddg=")) {
                java.net.URLDecoder.decode(link.substringAfter("uddg=").substringBefore("&"), "UTF-8")
            } else link

            candidates.add(cleanLink to "$title $snippet")
        }

        if (candidates.isEmpty()) return fallbackUrl

        // 3. Use LiteRT ML Model to Score the candidates
        if (interpreter != null) {
            var bestUrl = fallbackUrl
            var bestScore = -1f

            for ((url, text) in candidates) {
                val score = scoreWithModel(text)
                if (score > bestScore) {
                    bestScore = score
                    bestUrl = url
                }
            }
            return bestUrl
        }

        // If ML Model is missing, use a fallback heuristic: first .gov, .us, or .org domain
        return candidates.firstOrNull { (url, _) ->
            url.contains(".gov") || url.contains(".us") || url.contains("sheriff")
        }?.first ?: candidates.first().first
    }

    private fun scoreWithModel(text: String): Float {
        val currentInterpreter = interpreter ?: return 1.0f

        // Prepare input and output
        // For String models, input is often an array of Strings
        val input = arrayOf(text)
        val output = Array(1) { FloatArray(1) }

        try {
            currentInterpreter.run(input, output)
            val score = output[0][0]
            if (score == 0.0f) {
                DiagnosticLogger.log("Inference Insight: Model returned 0.0 for '$text'.", DiagnosticLogger.LogEntry.LogLevel.DEBUG)
            }
            return score
        } catch (e: Exception) {
            DiagnosticLogger.log("Inference Error: ${e.message}", DiagnosticLogger.LogEntry.LogLevel.ERROR)
            return 0.5f // Neutral fallback
        }
    }
}
