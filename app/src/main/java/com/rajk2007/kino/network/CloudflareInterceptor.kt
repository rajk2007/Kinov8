package com.rajk2007.kino.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * Lightweight Android equivalent of CloudStream's CloudflareKiller.
 *
 * MovieBox currently returns HTTP 441 for clients that do not present a browser
 * session. When that happens, a short-lived WebView executes the challenge,
 * captures the resulting cookies, and retries the original NiceHttp request.
 */
class CloudflareInterceptor(
    private val context: Context,
    private val browserHeaders: Map<String, String>,
    private val timeoutSeconds: Long = 20L
) : Interceptor {
    private val savedCookies = ConcurrentHashMap<String, String>()
    private val solvingHosts = ConcurrentHashMap.newKeySet<String>()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        savedCookies[host]?.takeIf { it.isNotBlank() }?.let { cookies ->
            return chain.proceed(request.newBuilder().header("Cookie", cookies).build())
        }

        val response = chain.proceed(request)
        if (!isCloudflareBlock(response)) return response
        response.close()

        if (solvingHosts.add(host)) {
            try {
                solveInWebView(request.url.toString(), host)
            } finally {
                solvingHosts.remove(host)
            }
        }

        val cookies = savedCookies[host] ?: return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("Cookie", cookies).build())
    }

    private fun isCloudflareBlock(response: Response): Boolean {
        val server = response.header("Server").orEmpty().lowercase()
        return response.code == 441 || ((response.code == 403 || response.code == 503) && server.contains("cloudflare"))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun solveInWebView(url: String, host: String) {
        val finished = CountDownLatch(1)
        val startedAt = System.currentTimeMillis()
        var webView: WebView? = null

        mainHandler.post {
            runCatching {
                val cookies = CookieManager.getInstance()
                cookies.setAcceptCookie(true)
                webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = browserHeaders["User-Agent"]
                    webViewClient = object : WebViewClient() {
                        private fun captureCookies() {
                            cookies.flush()
                            cookies.getCookie(url)?.takeIf { it.isNotBlank() }?.let { savedCookies[host] = it }
                        }

                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                            captureCookies()
                            val hasChallengeCookie = savedCookies[host]?.contains("cf_clearance") == true
                            if (hasChallengeCookie || System.currentTimeMillis() - startedAt > 2500L) finished.countDown()
                            else mainHandler.postDelayed({ captureCookies(); finished.countDown() }, 1200L)
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) finished.countDown()
                        }
                    }
                    loadUrl(url, browserHeaders.filterKeys { !it.equals("User-Agent", true) })
                }
            }.onFailure { finished.countDown() }
        }

        finished.await(timeoutSeconds, TimeUnit.SECONDS)
        mainHandler.post { webView?.stopLoading(); webView?.destroy() }
    }
}
