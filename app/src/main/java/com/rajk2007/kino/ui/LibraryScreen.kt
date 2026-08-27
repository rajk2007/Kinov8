package com.rajk2007.kino.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import coil.compose.AsyncImage
import com.rajk2007.kino.core.MediaType
import com.rajk2007.kino.core.SearchResponse
import com.rajk2007.kino.data.ContinueWatchingStore
import com.rajk2007.kino.data.WatchProgress
import com.rajk2007.kino.downloads.DownloadRepository
import androidx.media3.exoplayer.offline.Download
import kotlinx.coroutines.delay

@Composable
fun LibraryScreen(onBack: () -> Unit, onOpen: (SearchResponse) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var watchItems by remember { mutableStateOf(emptyList<WatchProgress>()) }
    var downloads by remember { mutableStateOf(emptyList<Download>()) }
    LaunchedEffect(Unit) {
        val store = ContinueWatchingStore(context)
        while (true) {
            watchItems = store.all()
            downloads = runCatching { DownloadRepository.snapshot(context) }.getOrDefault(emptyList())
            delay(1500)
        }
    }
    Scaffold(containerColor = KinoBlack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                    Column { Text("Library", fontSize = 24.sp, fontWeight = FontWeight.Black); Text("Your watch history and downloads", color = KinoMuted, fontSize = 12.sp) }
                }
            }
            item { SectionHeader("Continue Watching") }
            if (watchItems.isEmpty()) item { EmptyLibraryState("Start watching something and it will appear here.") }
            else item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)) {
                    items(watchItems, key = { "${it.apiName}:${it.url}" }) { progress -> WatchCard(progress) { onOpen(SearchResponse(progress.name, progress.url, progress.apiName, MediaType.MOVIE, progress.posterUrl)) } }
                }
            }
            item { SectionHeader("Downloads") }
            if (downloads.isEmpty()) item { EmptyLibraryState("Selected downloads will appear here.") }
            else items(downloads, key = { it.request.id }) { download -> DownloadRow(download) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun WatchCard(progress: WatchProgress, onOpen: () -> Unit) {
    Column(Modifier.width(176.dp).clip(RoundedCornerShape(14.dp)).background(KinoSurface).clickable { onOpen() }) {
        Box(Modifier.fillMaxWidth().height(112.dp)) {
            AsyncImage(progress.posterUrl, progress.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.align(Alignment.Center).size(34.dp).clip(RoundedCornerShape(50)).background(KinoBlack.copy(alpha = .72f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = KinoGold) }
        }
        Column(Modifier.padding(10.dp)) {
            Text(progress.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp)); Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(4.dp)).background(KinoSurface2)) { Box(Modifier.fillMaxWidth(progress.percent / 100f).height(3.dp).background(KinoGold)) }
            Spacer(Modifier.height(5.dp)); Text("${progress.percent}% watched", color = KinoMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DownloadRow(download: Download) {
    val status = when (download.state) { Download.STATE_COMPLETED -> "Ready offline"; Download.STATE_DOWNLOADING -> "Downloading ${download.percentDownloaded.toInt().coerceAtLeast(0)}%"; Download.STATE_QUEUED -> "Queued"; Download.STATE_STOPPED -> "Paused"; Download.STATE_FAILED -> "Failed"; else -> "Preparing" }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp).clip(RoundedCornerShape(14.dp)).background(KinoSurface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CloudDownload, null, tint = if (download.state == Download.STATE_COMPLETED) KinoGold else KinoMuted, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(download.request.id.removePrefix("KINO:"), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(status, color = KinoMuted, fontSize = 12.sp) }
    }
}

@Composable private fun EmptyLibraryState(message: String) { Text(message, color = KinoMuted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
