package com.jamsholat2.android.util

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.CookieManager
import android.webkit.WebView
import kotlinx.serialization.decodeFromString
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Fetches data from Unsplash through a (hidden) WebView using the browser's own
 * network stack (Chromium TLS/DNS), which works even when the platform stack used
 * by OkHttp/DownloadManager cannot reach images.unsplash.com on some TV boxes.
 *
 * - [fetch] retrieves a page body as text (used for the napi search JSON, which is
 *   gated by an anti-bot JS challenge that Chromium solves transparently).
 * - [fetchBytes] downloads binary content via XHR and returns raw bytes.
 *
 * Must be created and used on the main thread. Callbacks arrive on the main thread.
 */
@SuppressLint("SetJavaScriptEnabled")
class UnsplashWebFetcher(private val webView: WebView) {

    private val handler = Handler(Looper.getMainLooper())
    private var jobId = 0
    private var activeCallback: ((String?) -> Unit)? = null
    private var deadlineMs = 0L
    private var polling = false

    private val byteCallbacks = ConcurrentHashMap<String, (ByteArray?, String?) -> Unit>()

    private val jsBridge = object {
        @JavascriptInterface
        fun onData(requestId: String, payload: String) {
            val callback = byteCallbacks.remove(requestId) ?: return
            handler.post {
                if (payload.startsWith("ERR:")) {
                    callback(null, payload.removePrefix("ERR:").ifBlank { "gagal memuat" })
                } else {
                    try {
                        val b64 = payload.removePrefix("OK:")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        if (bytes.isEmpty()) callback(null, "data kosong")
                        else callback(bytes, null)
                    } catch (e: Exception) {
                        callback(null, e.message ?: "gagal decode data")
                    }
                }
            }
        }
    }

    init {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.addJavascriptInterface(jsBridge, BRIDGE_NAME)
    }

    /**
     * Loads [url] in the WebView and returns the final page body text if it looks
     * like a JSON object, or null on timeout. Cancels any in-flight request.
     */
    fun fetch(url: String, onResult: (String?) -> Unit) {
        cancelActive()
        jobId++
        activeCallback = onResult
        deadlineMs = System.currentTimeMillis() + TIMEOUT_MS
        polling = true
        webView.loadUrl(url)
        scheduleNextPoll(jobId)
    }

    /**
     * Downloads [url] via XHR inside the WebView and returns the bytes.
     * Multiple requests may run concurrently. [onResult] receives (bytes, null)
     * on success or (null, errorMessage) on failure/timeout.
     */
    fun fetchBytes(url: String, timeoutMs: Long = 45_000L, onResult: (ByteArray?, String?) -> Unit) {
        val requestId = UUID.randomUUID().toString().replace("-", "").take(16)
        byteCallbacks[requestId] = onResult
        handler.postDelayed({
            if (byteCallbacks.remove(requestId) != null) {
                onResult(null, "waktu habis")
            }
        }, timeoutMs)

        val safeUrl = url.replace("\\", "\\\\").replace("'", "\\'")
        val script = """
            (function(){
              function done(err, dataUrl){
                try { $BRIDGE_NAME.onData('$requestId', err ? 'ERR:'+err : 'OK:'+String(dataUrl).split(',')[1]); }
                catch(e){ try { $BRIDGE_NAME.onData('$requestId', 'ERR:'+e.message); } catch(_){} }
              }
              try{
                var x = new XMLHttpRequest();
                x.open('GET', '$safeUrl', true);
                x.responseType = 'arraybuffer';
                x.timeout = ${timeoutMs - 5000};
                x.onload = function(){
                  if (x.status >= 200 && x.status < 300 && x.response) {
                    var reader = new FileReader();
                    reader.onload = function(){ done(null, reader.result); };
                    reader.onerror = function(){ done('gagal membaca data'); };
                    reader.readAsDataURL(new Blob([x.response]));
                  } else {
                    done('HTTP ' + x.status);
                  }
                };
                x.onerror = function(){ done('kesalahan jaringan'); };
                x.ontimeout = function(){ done('waktu habis (xhr)'); };
                x.send();
              } catch(e){ done(String(e.message || e)); }
            })();
        """.trimIndent()
        try {
            webView.evaluateJavascript(script, null)
        } catch (e: Exception) {
            if (byteCallbacks.remove(requestId) != null) {
                onResult(null, e.message ?: "WebView tidak siap")
            }
        }
    }

    fun cancelActive() {
        jobId++
        polling = false
        handler.removeCallbacksAndMessages(null)
        activeCallback = null
    }

    /** Cancels all pending byte requests. The hidden WebView itself is destroyed by its host. */
    fun destroy() {
        cancelActive()
        val ids = byteCallbacks.keys.toList()
        for (id in ids) {
            byteCallbacks.remove(id)?.invoke(null, "dibatalkan")
        }
    }

    private fun scheduleNextPoll(currentJob: Int) {
        handler.postDelayed({
            if (currentJob != jobId || !polling) return@postDelayed
            val bodyScript = "(document.body && document.body.innerText) || ''"
            try {
                webView.evaluateJavascript(bodyScript) { encoded ->
                    if (currentJob != jobId) return@evaluateJavascript
                    val text = try {
                        UnsplashClient.json.decodeFromString<String>(encoded ?: "\"\"")
                    } catch (_: Exception) {
                        ""
                    }
                    when {
                        text.trimStart().startsWith("{") -> finish(currentJob, text)
                        System.currentTimeMillis() >= deadlineMs -> finish(currentJob, null)
                        else -> scheduleNextPoll(currentJob)
                    }
                }
            } catch (_: Exception) {
                // WebView may be navigating/destroyed; keep waiting until deadline
                if (System.currentTimeMillis() >= deadlineMs) finish(currentJob, null)
                else scheduleNextPoll(currentJob)
            }
        }, POLL_INTERVAL_MS)
    }

    private fun finish(currentJob: Int, result: String?) {
        if (currentJob != jobId) return
        polling = false
        handler.removeCallbacksAndMessages(null)
        val callback = activeCallback
        activeCallback = null
        callback?.invoke(result)
    }

    companion object {
        private const val TIMEOUT_MS = 30_000L
        private const val POLL_INTERVAL_MS = 600L
        private const val BRIDGE_NAME = "__unsplashBridge"

        /** Size for the hidden hosting view; big enough for JS engines to run reliably. */
        const val HOST_VIEW_SIZE_DP = 1
    }
}
