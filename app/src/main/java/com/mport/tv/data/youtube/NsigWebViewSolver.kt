package com.mport.tv.data.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Runs assets/nsigsolver/solver.html in a WebView (same bridge as ion-tv style).
 * solveN(playerJs, nChallenge) → onSolved / onError via AndroidBridge.
 */
class NsigWebViewSolver(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var playerJsCache: String? = null
    @Volatile private var playerJsUrlCache: String? = null

    suspend fun solveN(nChallenge: String, playerJs: String? = null): String =
        withTimeout(25_000) {
            val js = playerJs ?: playerJsCache ?: fetchDefaultPlayerJs().also { playerJsCache = it }
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val webView = WebView(context.applicationContext)
                    val solved = AtomicReference<String?>(null)
                    val error = AtomicReference<String?>(null)

                    @SuppressLint("SetJavaScriptEnabled")
                    fun setup() {
                        webView.settings.javaScriptEnabled = true
                        webView.settings.domStorageEnabled = true
                        webView.addJavascriptInterface(object {
                            @JavascriptInterface
                            fun getPlayerJs(): String = js

                            @JavascriptInterface
                            fun onSolved(challenge: String, result: String) {
                                if (challenge == nChallenge || solved.get() == null) {
                                    solved.set(result)
                                    mainHandler.post {
                                        if (cont.isActive) cont.resume(result)
                                        destroy(webView)
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onError(msg: String) {
                                error.set(msg)
                                mainHandler.post {
                                    if (cont.isActive) {
                                        cont.resumeWithException(
                                            IllegalStateException("nsig error: $msg")
                                        )
                                    }
                                    destroy(webView)
                                }
                            }
                        }, "AndroidBridge")

                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val escapedChallenge = nChallenge
                                    .replace("\\", "\\\\")
                                    .replace("'", "\\'")
                                // player JS already available via AndroidBridge.getPlayerJs()
                                view?.evaluateJavascript(
                                    "solveN(null, '$escapedChallenge')"
                                ) { value ->
                                    // sync return path
                                    val cleaned = value?.trim()?.removeSurrounding("\"")
                                        ?.replace("\\\"", "\"")
                                        ?.replace("\\\\", "\\")
                                    if (!cleaned.isNullOrBlank() &&
                                        cleaned != "null" &&
                                        cleaned != "undefined" &&
                                        cont.isActive &&
                                        solved.get() == null
                                    ) {
                                        solved.set(cleaned)
                                        cont.resume(cleaned)
                                        destroy(webView)
                                    }
                                }
                            }
                        }
                        webView.loadUrl("file:///android_asset/nsigsolver/solver.html")
                    }

                    cont.invokeOnCancellation { destroy(webView) }
                    setup()
                }
            }
        }

    suspend fun ensurePlayerJs(videoId: String? = null): String {
        playerJsCache?.let { return it }
        val url = discoverPlayerJsUrl(videoId)
        playerJsUrlCache = url
        val body = downloadText(url)
        playerJsCache = body
        return body
    }

    private suspend fun fetchDefaultPlayerJs(): String = ensurePlayerJs(null)

    private suspend fun discoverPlayerJsUrl(videoId: String?): String = withContext(Dispatchers.IO) {
        val watchUrl = if (!videoId.isNullOrBlank()) {
            "https://www.youtube.com/watch?v=$videoId"
        } else {
            "https://www.youtube.com/"
        }
        val html = downloadText(watchUrl)
        val regex = Regex("""/s/player/([a-zA-Z0-9_-]+)/player_ias\.vflset/[^"]+/base\.js""")
        val m = regex.find(html)
            ?: Regex("""/s/player/([a-zA-Z0-9_-]+)/.*?base\.js""").find(html)
        val path = m?.value
            ?: throw IllegalStateException("Player base.js URL not found on YouTube page")
        if (path.startsWith("http")) path else "https://www.youtube.com$path"
    }

    private fun downloadText(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 45_000
        conn.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        )
        try {
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode} for $url")
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun destroy(webView: WebView) {
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (_: Exception) {
        }
    }
}
