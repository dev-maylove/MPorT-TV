package com.mport.tv.data.youtube

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder

/**
 * Option 3 integration: after InnerTube player response, fix signatureCipher / n-param
 * using NsigWebViewSolver assets from ion-tv.
 */
class YoutubeStreamResolver(
    context: Context,
    private val client: YoutubeInnerTubeClient = YoutubeInnerTubeClient()
) {
    private val nsig = NsigWebViewSolver(context.applicationContext)
    private val poToken = PoTokenWebViewHelper(context.applicationContext)

    suspend fun resolvePlayable(videoId: String): YoutubePlayable {
        val playable = client.getPlayer(videoId).getOrThrow()
        if (!playable.needsNsig() && !playable.bestDirectUrl().isNullOrBlank()) {
            return playable
        }

        val playerJs = runCatching { nsig.ensurePlayerJs(videoId) }.getOrNull()

        val fixedProgressive = playable.progressiveFormats.map {
            fixFormat(it, playerJs)
        }
        val fixedAdaptive = playable.adaptiveFormats.map {
            fixFormat(it, playerJs)
        }

        // Soft PO attempt (may no-op if att challenge incomplete)
        runCatching { poToken.tryGeneratePoToken(videoId) }

        return playable.copy(
            progressiveFormats = fixedProgressive,
            adaptiveFormats = fixedAdaptive
        )
    }

    private suspend fun fixFormat(format: StreamFormat, playerJs: String?): StreamFormat {
        var url = format.url
        val cipher = format.signatureCipher

        if (url.isNullOrBlank() && !cipher.isNullOrBlank()) {
            val params = parseQuery(cipher)
            val base = params["url"] ?: return format
            val s = params["s"]
            val sp = params["sp"] ?: "sig"
            url = if (s != null) "$base&$sp=$s" else base
        }

        if (url.isNullOrBlank()) return format

        // Decode n= throttling param with nsig solver when player JS available
        val uri = Uri.parse(url)
        val nVal = uri.getQueryParameter("n")
        if (!nVal.isNullOrBlank() && playerJs != null) {
            val solved = runCatching { nsig.solveN(nVal, playerJs) }.getOrNull()
            if (!solved.isNullOrBlank()) {
                val builder = uri.buildUpon().clearQuery()
                val names = uri.queryParameterNames
                for (name in names) {
                    if (name == "n") {
                        builder.appendQueryParameter("n", solved)
                    } else {
                        uri.getQueryParameters(name).forEach {
                            builder.appendQueryParameter(name, it)
                        }
                    }
                }
                url = builder.build().toString()
            }
        }

        return format.copy(url = url)
    }

    private fun parseQuery(cipher: String): Map<String, String> {
        return cipher.split("&").mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null
            else {
                val k = URLDecoder.decode(part.substring(0, idx), "UTF-8")
                val v = URLDecoder.decode(part.substring(idx + 1), "UTF-8")
                k to v
            }
        }.toMap()
    }
}
