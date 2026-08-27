package com.rajk2007.kino.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Lightweight persistence for playback history; no server or TMDB dependency. */
data class WatchProgress(
    val url: String,
    val apiName: String,
    val name: String,
    val posterUrl: String?,
    val position: Long,
    val duration: Long,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val percent: Int get() = if (duration > 0) ((position * 100) / duration).toInt().coerceIn(0, 100) else 0
}

class ContinueWatchingStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun save(progress: WatchProgress) {
        val entries = all().filterNot { it.url == progress.url && it.apiName == progress.apiName }
            .toMutableList()
        entries.add(0, progress.copy(updatedAt = System.currentTimeMillis()))
        val json = JSONArray().apply {
            entries.take(MAX_ITEMS).forEach { item ->
                put(JSONObject().apply {
                    put("url", item.url)
                    put("apiName", item.apiName)
                    put("name", item.name)
                    put("posterUrl", item.posterUrl ?: JSONObject.NULL)
                    put("position", item.position)
                    put("duration", item.duration)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
        preferences.edit().putString(KEY_ENTRIES, json.toString()).apply()
    }

    @Synchronized
    fun all(): List<WatchProgress> {
        val raw = preferences.getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.optJSONObject(index) ?: continue
                    add(WatchProgress(
                        url = item.optString("url"), apiName = item.optString("apiName", "MovieBox"),
                        name = item.optString("name", "Untitled"), posterUrl = item.optString("posterUrl").takeIf { it.isNotBlank() && it != "null" },
                        position = item.optLong("position"), duration = item.optLong("duration"), updatedAt = item.optLong("updatedAt")
                    ))
                }
            }.filter { it.url.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun remove(url: String, apiName: String) {
        val remaining = all().filterNot { it.url == url && it.apiName == apiName }
        val json = JSONArray().apply { remaining.forEach { put(JSONObject().apply { put("url", it.url); put("apiName", it.apiName); put("name", it.name); put("posterUrl", it.posterUrl ?: JSONObject.NULL); put("position", it.position); put("duration", it.duration); put("updatedAt", it.updatedAt) }) } }
        preferences.edit().putString(KEY_ENTRIES, json.toString()).apply()
    }

    companion object { private const val FILE_NAME = "kino_library"; private const val KEY_ENTRIES = "watch_progress"; private const val MAX_ITEMS = 20 }
}
