package com.hereliesaz.cleanunderwear.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.model.AzButtonShape
import com.hereliesaz.cleanunderwear.domain.BrowserMission
import com.hereliesaz.cleanunderwear.util.DiagnosticLogger

/**
 * User-visible WebView for missions that require the user's real browser
 * session (cookies, captcha, login). Unlike [com.hereliesaz.cleanunderwear.network.WebViewScraper]
 * — which spawns offscreen WebViews with a spoofed Chrome UA — this screen
 * uses the device's default UA, accepts third-party cookies, and is rendered
 * full-screen so the user can see/intervene.
 *
 * Lifecycle:
 *   - On compose: build a WebView, configure it, persist cookies via
 *     CookieManager, load mission.initialUrl.
 *   - User reads the page, completes any login / captcha.
 *   - User taps "Run Automation" → mission.extractionScript is evaluated and
 *     the result is reported via [onComplete].
 *   - User taps Done (or back) → [onCancel].
 *   - On disposal: WebView.destroy() and CookieManager.flush() preserve the
 *     session for the next mission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun BrowserScreen(
    mission: BrowserMission,
    onComplete: (String?) -> Unit,
    onCancel: () -> Unit
) {
    var pageReady by remember { mutableStateOf(false) }
    var automationRan by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
            webView?.let {
                try {
                    it.stopLoading()
                    it.destroy()
                } catch (e: Exception) {
                    DiagnosticLogger.log("BrowserScreen: WebView destroy failed: ${e.message}")
                }
            }
            webView = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mission.label) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->
                    val cookies = CookieManager.getInstance()
                    cookies.setAcceptCookie(true)

                    val view = WebView(context).apply {
                        cookies.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            // Default UA: this is a real, user-facing browser
                            // session — do NOT spoof Chrome the way the covert
                            // scraper does.
                        }

                        addJavascriptInterface(
                            HtmlDumpInterface { html ->
                                onComplete(html)
                            },
                            "HTMLOUT"
                        )

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageReady = true
                                DiagnosticLogger.log("BrowserScreen: loaded $url")
                            }
                        }

                        loadUrl(mission.initialUrl)
                    }
                    webView = view
                    view
                }
            )

            AzButton(
                text = if (!pageReady) "Loading…"
                    else if (automationRan) "Running…"
                    else "Run Automation",
                onClick = {
                    if (pageReady && !automationRan) {
                        automationRan = true
                        webView?.evaluateJavascript(mission.extractionScript, null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = AzButtonShape.RECTANGLE
            )
        }
    }
}

private class HtmlDumpInterface(private val onResult: (String?) -> Unit) {
    @JavascriptInterface
    fun dump(html: String?) {
        onResult(html)
    }
}
