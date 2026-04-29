
package com.hereliesaz.cleanunderwear.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A Trojan horse for Cloudflare's digital bouncer. 
 * We spawn an invisible browser on the main thread, let it patiently swallow 
 * the JavaScript challenges, and then ruthlessly extract the DOM.
 */
@Singleton
class WebViewScraper @Inject constructor(@ApplicationContext private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    suspend fun scrapeGhostTown(url: String): Document? = withContext(Dispatchers.Main) {
        DiagnosticLogger.log("Opening Covert Browser for: $url")
        withTimeoutOrNull(30000L) { // 30 second timeout for the entire operation
            suspendCoroutine { continuation ->
                val webView = WebView(context)
                var isResumed = false

                fun resumeOnce(doc: Document?) {
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(doc)
                        webView.post { 
                            webView.stopLoading()
                            webView.destroy() 
                        }
                    }
                }

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"
                }

                class HtmlDumpInterface {
                    @JavascriptInterface
                    fun dump(html: String) {
                        try {
                            DiagnosticLogger.log("Intelligence extracted from DOM.")
                            resumeOnce(Jsoup.parse(html))
                        } catch (e: Exception) {
                            resumeOnce(null)
                        }
                    }
                }

                webView.addJavascriptInterface(HtmlDumpInterface(), "HTMLOUT")

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        DiagnosticLogger.log("Covert page loaded. Analyzing content...")
                        view?.postDelayed({
                            try {
                                view.evaluateJavascript(
                                    "(function() { window.HTMLOUT.dump(document.documentElement.outerHTML); })();",
                                    null
                                )
                            } catch (e: Exception) {
                                resumeOnce(null)
                            }
                        }, 1000) // Reduced delay
                    }
                    
                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?
                    ) {
                        if (request?.isForMainFrame == true && errorResponse?.statusCode != 200 && errorResponse?.statusCode != 403) {
                           DiagnosticLogger.log("Intelligence Alert: Source returned HTTP ${errorResponse?.statusCode}", DiagnosticLogger.LogEntry.LogLevel.WARN)
                           resumeOnce(null)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        DiagnosticLogger.log("Intelligence Alert: Web failure ($errorCode: $description)", DiagnosticLogger.LogEntry.LogLevel.ERROR)
                        resumeOnce(null)
                    }
                }

                webView.loadUrl(url)
            }
        }
    }
}
