package com.hereliesaz.cleanunderwear.util

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.PrintWriter
import java.io.StringWriter

/**
 * A digital canary. When the app dies, it screams into the GitHub abyss.
 */
class GitHubCrashReporter(private val context: Context) {

    private val client = OkHttpClient()
    private val REPO_OWNER = "HereLiesAz"
    private val REPO_NAME = "CleanUnderwear"
    
    // In a real production environment, this would be an encrypted secret or fetched from a secure vault.
    // For this implementation, we scaffold the mechanism.
    private val GITHUB_TOKEN = "secrets.GH_TOKEN"

    fun reportCrash(throwable: Throwable) {
        if (GITHUB_TOKEN.isBlank()) {
            Log.e("CrashReporter", "GitHub Token is missing. Cannot report crash.")
            return
        }

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val bodyJson = JSONObject().apply {
            put("title", "Crash Report: ${throwable.message ?: "Unknown Error"}")
            put("body", """
                ### Crash Detected
                **Message:** ${throwable.message}
                **Device:** ${android.os.Build.MODEL} (${android.os.Build.VERSION.RELEASE})
                
                ```stacktrace
                $stackTrace
                ```
            """.trimIndent())
            put("labels", listOf("bug", "crash"))
        }

        val requestBody = bodyJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/issues")
            .header("Authorization", "Bearer $GITHUB_TOKEN")
            .header("Accept", "application/vnd.github+json")
            .post(requestBody)
            .build()

        // We use a background thread to avoid blocking the main thread during a crash
        // though the app is dying anyway.
        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d("CrashReporter", "Crash reported successfully.")
                } else {
                    Log.e("CrashReporter", "Failed to report crash: ${response.code} ${response.body?.string()}")
                }
            } catch (e: Exception) {
                Log.e("CrashReporter", "Error sending crash report", e)
            }
        }.start()
    }
}
