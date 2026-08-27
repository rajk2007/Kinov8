package com.rajk2007.kino.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Minimal native equivalents of the provider contracts used by MovieBox. */
enum class MediaType { MOVIE, TV_SERIES }

data class SearchResponse(
    val name: String,
    val url: String,
    val apiName: String,
    val type: MediaType = MediaType.MOVIE,
    val posterUrl: String? = null
)

data class Episode(
    val id: String,
    val name: String,
    val season: Int,
    val episode: Int,
    val posterUrl: String? = null
)

data class ExtractorLink(
    val source: String,
    val name: String,
    val url: String,
    val type: StreamType,
    val quality: Int? = null,
    val headers: Map<String, String> = emptyMap()
)

enum class StreamType { VIDEO, M3U8, DASH, MAGNET, TORRENT }

data class LoadResponse(
    val name: String,
    val url: String,
    val apiName: String,
    val type: MediaType,
    val posterUrl: String? = null,
    val backgroundPosterUrl: String? = null,
    val plot: String? = null,
    val year: Int? = null,
    val rating: Int? = null,
    val tags: List<String> = emptyList(),
    val durationMinutes: Int? = null,
    val episodes: List<Episode> = emptyList(),
    val data: String
)

data class HomeSection(val title: String, val items: List<SearchResponse>)

abstract class MainAPI {
    abstract val name: String
    abstract val mainUrl: String
    abstract suspend fun getMainPage(): List<HomeSection>
    abstract suspend fun search(query: String): List<SearchResponse>
    abstract suspend fun load(url: String): LoadResponse?
    abstract suspend fun loadLinks(data: String): List<ExtractorLink>
}

suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
