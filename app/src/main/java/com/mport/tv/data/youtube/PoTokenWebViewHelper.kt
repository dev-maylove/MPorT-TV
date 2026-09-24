package com.mport.tv.data.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Loads assets/potokennp2/po_token.html (BotGuard helpers from ion-tv style assets).
 * Full PO flow: att request → BotGuard challenge → snapshot → integrity token → mint PO token.
 *
 * This helper exposes the JS functions in the HTML and a minimal att client.
 * YouTube may still change endpoints; treat failures as soft (playback can continue without PO).
 */
class PoTokenWebViewHelper(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Attempt to mint a PO token for [identifier] (often visitorData or video binding).
     * Returns base64url token or null on failure.
     */
    suspend fun tryGeneratePoToken(identifier: String): String? = runCatching {
        withTimeout(40_000) {
            // Minimal path: load HTML and verify obtainPoToken exists; full BotGuard needs live challenge.
            val challenge = fetchAttChallenge()
            runInWebView(challenge, identifier)
        }
    }.getOrNull()

    private suspend fun fetchAttChallenge(): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            // Public att endpoint used by various clients (may require cookies / keys over time).
            val url = "https://www.youtube.com/youtubei/v1/att/get?prettyPrint=false&key=${YoutubeInnerTubeClient.DEFAULT_API_KEY}"
            val body = JSONObject()
                .put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject()
                            .put("clientName", "WEB")
                            .put("clientVersion", "2.20240101.00.00")
                            .put("hl", "en")
                    )
                )
                .put("engagementType", "ENGAGEMENT_TYPE_UNBOUND")
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            try {
                conn.outputStream.use {
                    it.write(body.toString().toByteArray(StandardCharsets.UTF_8))
                }
                val text = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: return@runCatching null
                JSONObject(text)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun runInWebView(challengeJson: JSONObject?, identifier: String): String =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val webView = WebView(context.applicationContext)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true

                webView.addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onPoToken(b64: String) {
                        mainHandler.post {
                            if (cont.isActive) cont.resume(b64)
                            destroy(webView)
                        }
                    }

                    @JavascriptInterface
                    fun onError(msg: String) {
                        mainHandler.post {
                            if (cont.isActive) {
                                cont.resumeWithException(IllegalStateException(msg))
                            }
                            destroy(webView)
                        }
                    }
                }, "PoBridge")

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val challengeArg = if (challengeJson != null) {
                            JSONObject.quote(challengeJson.toString())
                        } else {
                            "null"
                        }
                        val idArg = JSONObject.quote(identifier)
                        // Attempt runBotGuard if challenge present; otherwise report limited mode.
                        val script = """
                            (function(){
                              try {
                                if (typeof runBotGuard !== 'function' || typeof obtainPoToken !== 'function') {
                                  PoBridge.onError('PO helpers missing');
                                  return;
                                }
                                var challenge = $challengeArg;
                                if (!challenge) {
                                  PoBridge.onError('No att challenge');
                                  return;
                                }
                                var data = typeof challenge === 'string' ? JSON.parse(challenge) : challenge;
                                // Shape varies; pass through to runBotGuard when fields exist
                                runBotGuard(data).then(function(r) {
                                  // integrity token still required from YouTube for full mint —
                                  // without it we only confirm BG path works
                                  PoBridge.onError('BotGuard OK but integrity token step needs live session');
                                }).catch(function(e) {
                                  PoBridge.onError(String(e && e.message ? e.message : e));
                                });
                              } catch(e) {
                                PoBridge.onError(String(e));
                              }
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(script, null)
                    }
                }

                cont.invokeOnCancellation { destroy(webView) }
                webView.loadUrl("file:///android_asset/potokennp2/po_token.html")
            }
        }

    private fun destroy(webView: WebView) {
        try {
            webView.stopLoading()
            webView.destroy()
        } catch (_: Exception) {
        }
    }

    companion object {
        fun bytesToBase64Url(bytes: ByteArray): String =
            Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
