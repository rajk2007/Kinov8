package com.rajk2007.kino.network

import android.content.Context
import com.lagradost.nicehttp.Session
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared CloudStream-style NiceHttp client.
 *
 * NiceHttp 0.4.2 exposes Requests/Session rather than a NiceHttp class. Session
 * is the correct equivalent here: it wraps OkHttp with NiceHttp request helpers
 * and retains cookies between requests, which is important for Cloudflare checks.
 */
object Network {
    val browserHeaders: Map<String, String> = mapOf(
        "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language" to "en-US,en;q=0.5",
        "Connection" to "keep-alive"
    )

    private var appInstance: Session? = null

    /** Initialize once from Application/Activity before any provider request. */
    @Synchronized
    fun initialize(context: Context) {
        if (appInstance != null) return
        val appContext = context.applicationContext
        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(CloudflareInterceptor(appContext, browserHeaders))
            .build()
        appInstance = Session(client).apply {
            defaultHeaders = browserHeaders
            defaultTimeOut = 45
        }
    }

    /** The single configured client used by MovieBoxProvider for every request. */
    val app: Session
        get() = appInstance ?: error("Network.initialize(context) must be called before using Network.app")
}
