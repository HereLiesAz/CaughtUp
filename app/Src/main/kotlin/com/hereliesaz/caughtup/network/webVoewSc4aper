
package com.hereliesaz.caughtup.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A Trojan horse for Cloudflare's digital bouncer. 
 * We spawn an invisible browser on the main thread, let it patiently swallow 
 * the JavaScript challenges, and then ruthlessly extract the DOM.
 */
class WebViewScraper(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    suspend fun scrapeGhostTown(url: String): Document? = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                // Disguise the dragnet as a bored local doomscrolling on a Pixel
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"
            }

            class HtmlDumpInterface {
                @JavascriptInterface
                fun dump(html: String) {
                    try {
                        continuation.resume(Jsoup.parse(html))
                    } catch (e: Exception) {
                        continuation.resume(null)
                    } finally {
                        webView.destroy()
                    }
                }
            }

            webView.addJavascriptInterface(HtmlDumpInterface(), "HTMLOUT")

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Give Turnstile a few seconds of bureaucratic waiting to decide we have a pulse
                    view?.postDelayed({
                        view.evaluateJavascript(
                            "(function() { window.HTMLOUT.dump(document.documentElement.outerHTML); })();",
                            null
                        )
                    }, 5000) 
                }
                
                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true && errorResponse?.statusCode != 200 && errorResponse?.statusCode != 403) {
                       // Cloudflare often serves the JS challenge on a 403. We ignore it and wait.
                       continuation.resume(null)
                       webView.destroy()
                    }
                }
            }

            webView.loadUrl(url)
        }
    }
}
