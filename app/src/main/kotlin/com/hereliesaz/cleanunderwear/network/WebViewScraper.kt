
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A Trojan horse for Cloudflare's digital bouncer. 
 * We spawn an invisible browser on the main thread, let it patiently swallow 
 * the JavaScript challenges, and then ruthlessly extract the DOM.
 */
@Singleton
class WebViewScraper @Inject constructor(@ApplicationContext private val context: Context) {

    private var htmlCallback: ((String) -> Unit)? = null

    inner class AndroidInterface {
        @JavascriptInterface
        fun processHtml(html: String) {
            htmlCallback?.invoke(html)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    suspend fun scrapeWithInjection(url: String, script: String): String? = withContext(Dispatchers.Main) {
        DiagnosticLogger.log("Launching Deep Harvest: $url")
        withTimeoutOrNull(120000L) { // 2 minute timeout for scrolling/loading
            suspendCancellableCoroutine { continuation ->
                val webView = WebView(context)
                var isResumed = false

                fun resumeOnce(html: String?) {
                    if (!isResumed) {
                        isResumed = true
                        htmlCallback = null
                        continuation.resume(html)
                        webView.post { 
                            try {
                                webView.stopLoading()
                                webView.destroy() 
                            } catch (e: Exception) {}
                        }
                    }
                }

                continuation.invokeOnCancellation { resumeOnce(null) }

                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"
                }

                webView.addJavascriptInterface(AndroidInterface(), "AndroidInterface")

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        DiagnosticLogger.log("Deep Harvest page ready. Injecting intelligence script...")
                        view?.evaluateJavascript(script, null)
                    }
                }

                htmlCallback = { html ->
                    DiagnosticLogger.log("Intelligence data extracted via script.")
                    resumeOnce(html)
                }

                webView.loadUrl(url)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    suspend fun scrapeGhostTown(url: String): Document? = withContext(Dispatchers.Main) {
        DiagnosticLogger.log("Opening Covert Browser for: $url")
        withTimeoutOrNull(30000L) { // 30 second timeout for the entire operation
            suspendCancellableCoroutine { continuation ->
                val webView = WebView(context)
                var isResumed = false

                fun resumeOnce(doc: Document?) {
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(doc)
                        webView.post { 
                            try {
                                webView.stopLoading()
                                webView.clearHistory()
                                webView.destroy() 
                            } catch (e: Exception) {}
                        }
                    }
                }
                
                continuation.invokeOnCancellation {
                    resumeOnce(null)
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
