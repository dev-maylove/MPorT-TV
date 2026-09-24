package com.mport.tv.data.youtube

/**
 * Option 3 — nsig + PO token assets bundled from ion-tv APK extraction.
 *
 * Assets:
 * - assets/nsigsolver/ (solver.html, yt.solver.core.js, meriyah, astring, polyfill, es5transform)
 * - assets/potokennp2/ (po_token.html, po_token2.html, v8/po_token.js, polyfill)
 *
 * Runtime:
 * - [NsigWebViewSolver] loads solver.html, AndroidBridge.getPlayerJs / onSolved / onError
 * - [PoTokenWebViewHelper] loads po_token.html + att challenge attempt
 * - [YoutubeStreamResolver] applies nsig to `n=` query on stream URLs
 */
object YoutubeNsigNotes {
    const val STATUS = "bundled"
    const val MESSAGE =
        "nsigsolver + potokennp2 assets included. nsig applied via WebView; PO token needs live BotGuard/integrity session and may soft-fail."
}
