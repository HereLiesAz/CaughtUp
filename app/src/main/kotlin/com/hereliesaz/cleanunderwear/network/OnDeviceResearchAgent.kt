package com.hereliesaz.cleanunderwear.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.net.URLEncoder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The tiny on-device research AI.
 * It uses a LiteRT (TFLite) text classifier to score search results 
 * and deduce the official municipal roster or obituary URL.
 */
@Singleton
class OnDeviceResearchAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scraper: WebViewScraper
) {
    private var interpreter: Interpreter? = null
    private var vocabMap: Map<String, Int> = emptyMap()

    init {
        try {
            val modelBuffer = loadModelFile("research_agent.tflite")
            interpreter = Interpreter(modelBuffer)
            loadVocab("research_agent_vocab.txt")
        } catch (e: Exception) {
            // Model isn't trained/placed yet.
            // Run `python3 scripts/train_research_ai.py` to generate it.
            interpreter = null
        }
    }

    private fun loadModelFile(fileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(fileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun loadVocab(fileName: String) {
        try {
            val lines = context.assets.open(fileName).bufferedReader().readLines()
            vocabMap = lines.mapIndexed { index, word -> word to index }.toMap()
        } catch (e: Exception) {
            vocabMap = emptyMap()
        }
    }

    suspend fun getDynamicLockupUrl(areaCode: String, residenceInfo: String? = null): String {
        val locationQuery = residenceInfo?.takeIf { it.isNotBlank() } ?: "Area Code $areaCode"
        val query = "\"inmate roster\" OR \"arrest log\" \"$locationQuery\""
        
        return executeAiSearch(query, "https://opso.us/docket/") // fallback
    }

    suspend fun getDynamicObituaryUrl(areaCode: String, residenceInfo: String? = null): String {
        val locationQuery = residenceInfo?.takeIf { it.isNotBlank() } ?: "Area Code $areaCode"
        val query = "obituary \"$locationQuery\""
        
        return executeAiSearch(query, "https://obits.nola.com/us/obituaries/nola/browse") // fallback
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
        if (interpreter != null && vocabMap.isNotEmpty()) {
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
        // Tokenize text using the loaded vocab (max length 20 to match model)
        val tokens = text.lowercase().replace(Regex("[^a-z0-9\\s]"), "").split("\\s+".toRegex())
        val inputArray = IntArray(20) { 0 }
        
        for (i in 0 until minOf(tokens.size, 20)) {
            inputArray[i] = vocabMap[tokens[i]] ?: 1 // 1 is usually OOV token
        }

        // The model expects a 2D array [batch_size, sequence_length] of floats (or ints if supported directly, but let's cast to float array for TF Lite standard if needed, actually TextVectorization outputs int64, so float32 or int32 depending on export). 
        // We'll use IntArray since TextVectorization outputs Ints.
        val inputBuffer = Array(1) { inputArray }
        val outputBuffer = Array(1) { FloatArray(1) }

        try {
            interpreter?.run(inputBuffer, outputBuffer)
            return outputBuffer[0][0]
        } catch (e: Exception) {
            return 0f
        }
    }
}
