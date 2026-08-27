package com.rajk2007.kino.providers

import android.net.Uri
import com.rajk2007.kino.core.Episode
import com.rajk2007.kino.core.ExtractorLink
import com.rajk2007.kino.core.HomeSection
import com.rajk2007.kino.core.LoadResponse
import com.rajk2007.kino.core.MainAPI
import com.rajk2007.kino.core.MediaType
import com.rajk2007.kino.core.SearchResponse
import com.rajk2007.kino.core.StreamType
import com.rajk2007.kino.network.NetworkModule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * MovieBox's provider logic is embedded directly in KINO. The app does not use TMDB
 * or a second metadata service; IDs returned here are passed unchanged to load().
 */
class MovieBoxProvider : MainAPI() {
    override val name = "MovieBox"
    override val mainUrl = "https://api.inmoviebox.com"

    /** CloudStream-style NiceHttp Session with browser defaults and cookie persistence. */
    private val app = NetworkModule.app
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // These are the provider's public signing values, kept in the provider module so
    // all MovieBox requests use the same protocol as the upstream extension.
    private val secretKeyDefault = "NzZpUmwwN3MweFNOOWpxbUVXQXQ3OUVCSlp1bElRSXNWNjRGWnIyTw=="
    private val secretKeyAlt = "WHFuMm5uTzQxL0w5Mm04bVNMQQ=="

    private val clientInfo = """{"package_name":"com.community.mbox.in","version_name":"3.0.03.0529.03","version_code":50020042,"os":"android","os_version":"16","device_id":"kino-native","install_store":"ps","brand":"Google","model":"KINO","system_language":"en","net":"NETWORK_WIFI","region":"IN","timezone":"Asia/Calcutta","sp_code":""}"""

    private fun md5(input: ByteArray): String = MessageDigest.getInstance("MD5")
        .digest(input).joinToString("") { "%02x".format(it) }

    private fun clientToken(): String {
        val timestamp = System.currentTimeMillis().toString()
        return "$timestamp,${md5(timestamp.reversed().toByteArray())}"
    }

    private fun signature(method: String, accept: String, contentType: String, url: String, body: String? = null): String {
        val parsed = Uri.parse(url)
        val query = parsed.queryParameterNames.sorted().joinToString("&") { key ->
            parsed.getQueryParameters(key).joinToString("&") { value -> "$key=$value" }
        }
        val canonicalUrl = parsed.path.orEmpty() + if (query.isEmpty()) "" else "?$query"
        val bytes = body?.toByteArray(Charsets.UTF_8)
        val bodyHash = bytes?.let { md5(it.copyOfRange(0, minOf(it.size, 102400))) }.orEmpty()
        val timestamp = System.currentTimeMillis()
        val canonical = listOf(
            method.uppercase(), accept, contentType, bytes?.size?.toString().orEmpty(),
            timestamp.toString(), bodyHash, canonicalUrl
        ).joinToString("\n")
        val key = Base64.getDecoder().decode(secretKeyDefault)
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(key, "HmacMD5"))
        return "$timestamp|2|${Base64.getEncoder().encodeToString(mac.doFinal(canonical.toByteArray()))}"
    }

    private suspend fun request(method: String, path: String, body: String? = null, extraHeaders: Map<String, String> = emptyMap()): String {
        val url = if (path.startsWith("http")) path else "$mainUrl$path"
        val apiHeaders = mapOf(
            "content-type" to "application/json",
            "x-client-token" to clientToken(),
            "x-tr-signature" to signature(method, NetworkModule.browserHeaders.getValue("Accept"), "application/json; charset=utf-8", url, body),
            "x-client-info" to clientInfo,
            "x-client-status" to "0"
        )
        val headers = NetworkModule.browserHeaders + apiHeaders + extraHeaders
        val response = if (body != null) {
            app.post(url, headers = headers, requestBody = body.toRequestBody(jsonMediaType))
        } else {
            app.get(url, headers = headers)
        }
        if (!response.isSuccessful) error("MovieBox request failed: HTTP ${response.code}")
        return response.body?.string().orEmpty()
    }

    override suspend fun getMainPage(): List<HomeSection> {
        val channels = listOf(
            "1" to "Trending", "2" to "For You", "3" to "Hits",
            "4" to "Blockbusters", "5" to "Top Rated"
        )
        return channels.mapNotNull { (channel, title) ->
            runCatching {
                val payload = JSONObject().apply {
                    put("page", 1); put("perPage", 12); put("channelId", "1068")
                    put("rate", JSONArray().put("0").put("10")); put("genre", "All")
                    put("sort", if (channel == "1") "Trending" else "ForYou")
                    put("channel", channel)
                }
                val root = JSONObject(request("POST", "/wefeed-mobile-bff/subject-api/list", payload.toString()))
                val items = root.optJSONObject("data")?.optJSONArray("items") ?: JSONArray()
                HomeSection(title, parseItems(items))
            }.getOrNull()?.takeIf { it.items.isNotEmpty() }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        if (query.isBlank()) return getMainPage().firstOrNull()?.items.orEmpty()
        val payload = JSONObject().apply { put("page", 1); put("perPage", 20); put("keyword", query) }
        val root = JSONObject(request("POST", "/wefeed-mobile-bff/subject-api/search/v2", payload.toString()))
        val results = root.optJSONObject("data")?.optJSONArray("results") ?: JSONArray()
        return buildList {
            for (i in 0 until results.length()) {
                val subjects = results.optJSONObject(i)?.optJSONArray("subjects") ?: continue
                addAll(parseItems(subjects))
            }
        }
    }

    private fun parseItems(items: JSONArray): List<SearchResponse> = buildList {
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val id = item.optString("subjectId").takeIf { it.isNotBlank() } ?: continue
            val title = item.optString("title").takeIf { it.isNotBlank() } ?: continue
            val cover = item.optJSONObject("cover")?.optString("url")
            add(SearchResponse(title, id, name, if (item.optInt("subjectType", 1) == 2) MediaType.TV_SERIES else MediaType.MOVIE, cover))
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = if (url.contains("subjectId=")) {
            Uri.parse(url).getQueryParameter("subjectId") ?: url.substringAfterLast('/')
        } else url.substringAfterLast('/')
        val root = JSONObject(request("GET", "/wefeed-mobile-bff/subject-api/get?subjectId=$id"))
        val data = root.optJSONObject("data") ?: return null
        val title = data.optString("title").ifBlank { "Untitled" }
        val type = if (data.optInt("subjectType", 1) == 2) MediaType.TV_SERIES else MediaType.MOVIE
        val cover = data.optJSONObject("cover")?.optString("url")
        val release = data.optString("releaseDate")
        val duration = data.optString("duration").let { value ->
            Regex("(\\d+)h\\s*(\\d+)m").find(value)?.let { it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt() }
                ?: value.filter { it.isDigit() }.toIntOrNull()
        }
        val episodes = if (type == MediaType.TV_SERIES) loadEpisodes(id, cover) else emptyList()
        return LoadResponse(
            name = title, url = id, apiName = name, type = type, posterUrl = cover,
            backgroundPosterUrl = cover, plot = data.optString("description").orEmpty().ifBlank { null },
            year = release.take(4).toIntOrNull(), rating = (data.optDouble("imdbRatingValue", 0.0) * 10).toInt().takeIf { it > 0 },
            tags = data.optString("genre").split(',').map { it.trim() }.filter { it.isNotEmpty() },
            durationMinutes = duration, episodes = episodes, data = id
        )
    }

    private suspend fun loadEpisodes(id: String, cover: String?): List<Episode> = runCatching {
        val data = JSONObject(request("GET", "/wefeed-mobile-bff/subject-api/season-info?subjectId=$id")).optJSONObject("data")
        val seasons = data?.optJSONArray("seasons") ?: return@runCatching emptyList()
        buildList {
            for (i in 0 until seasons.length()) {
                val season = seasons.optJSONObject(i) ?: continue
                val seasonNo = season.optInt("se", 1)
                for (episodeNo in 1..season.optInt("maxEp", 1)) {
                    add(Episode("$id|$seasonNo|$episodeNo", "S${seasonNo}E$episodeNo", seasonNo, episodeNo, cover))
                }
            }
        }
    }.getOrDefault(listOf(Episode("$id|1|1", "Episode 1", 1, 1, cover)))

    override suspend fun loadLinks(data: String): List<ExtractorLink> {
        val parts = data.split('|')
        val subjectId = parts.first()
        val season = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val episode = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val root = JSONObject(request("GET", "/wefeed-mobile-bff/subject-api/play-info?subjectId=$subjectId&se=$season&ep=$episode"))
        val streams = root.optJSONObject("data")?.optJSONArray("streams") ?: JSONArray()
        return buildList {
            for (i in 0 until streams.length()) {
                val stream = streams.optJSONObject(i) ?: continue
                val streamUrl = stream.optString("url").takeIf { it.isNotBlank() } ?: continue
                val format = stream.optString("format")
                val type = when {
                    format.equals("HLS", true) || streamUrl.contains(".m3u8", true) -> StreamType.M3U8
                    streamUrl.contains(".mpd", true) -> StreamType.DASH
                    streamUrl.startsWith("magnet:", true) -> StreamType.MAGNET
                    else -> StreamType.VIDEO
                }
                val headers = buildMap {
                    put("Referer", mainUrl)
                    stream.optString("signCookie").takeIf { it.isNotBlank() }?.let { put("Cookie", it) }
                }
                add(ExtractorLink(name, "$name ${stream.optString("resolutions", "Auto")}", streamUrl, type, headers = headers))
            }
        }
    }
}
