package com.hereliesaz.cleanunderwear.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
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
            interpreter = Interpreter(modelBuffer)
        } catch (e: Exception) {
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

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }


    suspend fun getDynamicLockupUrl(areaCode: String, residenceInfo: String? = null): String {
        val locationQuery = residenceInfo?.takeIf { it.isNotBlank() } ?: "Area Code $areaCode"
        val stateTriggers = triggers.values.find { it.area_codes.contains(areaCode) }
        
        val siteConstraint = stateTriggers?.priority_sources?.joinToString(" OR ") { "site:$it" } ?: ""
        val query = if (siteConstraint.isNotBlank()) {
            "($siteConstraint) \"inmate roster\" OR \"arrest log\" \"$locationQuery\""
        } else {
            "\"inmate roster\" OR \"arrest log\" \"$locationQuery\""
        }
        
        val fallback = stateTriggers?.priority_sources?.firstOrNull { it.contains(".gov") || it.contains(".us") }
            ?: "https://www.google.com/search?q=inmate+roster+$areaCode"
            
        return executeAiSearch(query, fallback)
    }

    suspend fun getDynamicObituaryUrl(areaCode: String, residenceInfo: String? = null): String {
        val locationQuery = residenceInfo?.takeIf { it.isNotBlank() } ?: "Area Code $areaCode"
        val stateTriggers = triggers.values.find { it.area_codes.contains(areaCode) }
        
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
        if (interpreter == null) return 0f

        // The TextVectorization layer is inside the model, so we pass the raw string.
        val input = arrayOf(text)
        val output = Array(1) { FloatArray(1) }

        try {
            interpreter?.run(input, output)
            return output[0][0]
        } catch (e: Exception) {
            return 0f
        }
    }
}
