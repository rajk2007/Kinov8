package com.rajk2007.kino.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.rajk2007.kino.core.ExtractorLink
import com.rajk2007.kino.core.LoadResponse
import com.rajk2007.kino.data.ContinueWatchingStore
import com.rajk2007.kino.data.DetailsViewModel
import com.rajk2007.kino.data.WatchProgress
import com.rajk2007.kino.downloads.DownloadController

private enum class StreamAction { PLAY, DOWNLOAD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(url: String, apiName: String, onBack: () -> Unit, vm: DetailsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var pendingAction by remember { mutableStateOf<StreamAction?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<ExtractorLink?>(null) }
    LaunchedEffect(url, apiName) { vm.load(url) }
    LaunchedEffect(state.links, state.linksLoading, pendingAction) {
        if (pendingAction != null && !state.linksLoading && state.links.isNotEmpty()) showSheet = true
    }
    val response = state.response
    if (playing != null && response != null) {
        PlayerScreen(response, playing!!, onBack = { playing = null })
        return
    }
    Scaffold(containerColor = KinoBlack, modifier = Modifier.navigationBarsPadding()) { padding ->
        when {
            state.loading -> LoadingState("Loading details instantly from MovieBox")
            response == null -> ErrorState(state.error ?: "Details unavailable", onBack)
            else -> DetailsBody(
                response = response,
                linksLoading = state.linksLoading,
                onBack = onBack,
                onAction = { action ->
                    pendingAction = action
                    if (state.links.isNotEmpty()) showSheet = true
                    else vm.loadLinks(response.episodes.firstOrNull()?.id ?: response.data)
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
    if (showSheet && response != null) {
        StreamSelectorSheet(
            links = state.links,
            loading = state.linksLoading,
            onDismiss = { showSheet = false; pendingAction = null },
            onSelect = { link ->
                val action = pendingAction
                showSheet = false; pendingAction = null
                if (action == StreamAction.PLAY) playing = link
                if (action == StreamAction.DOWNLOAD) DownloadController.enqueue(response.name, link)
            }
        )
    }
}

@Composable
private fun DetailsBody(response: LoadResponse, linksLoading: Boolean, onBack: () -> Unit, onAction: (StreamAction) -> Unit, modifier: Modifier) {
    LazyColumn(Modifier.fillMaxSize().then(modifier)) {
        item {
            Box(Modifier.fillMaxWidth().height(410.dp)) {
                AsyncImage(response.backgroundPosterUrl ?: response.posterUrl, response.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, KinoBlack))))
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) { Icon(Icons.Default.ArrowBack, "Back") }
                Text(response.name, Modifier.align(Alignment.BottomStart).padding(22.dp), fontSize = 30.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { onAction(StreamAction.PLAY) }, enabled = !linksLoading, colors = ButtonDefaults.buttonColors(containerColor = KinoGold, contentColor = KinoBlack)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    Button(onClick = { onAction(StreamAction.DOWNLOAD) }, enabled = !linksLoading, colors = ButtonDefaults.buttonColors(containerColor = KinoSurface2, contentColor = Color.White)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Download") }
                }
                if (linksLoading) { Spacer(Modifier.height(12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(color = KinoGold, modifier = Modifier.size(17.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Finding languages and qualities...", color = KinoMuted, fontSize = 13.sp) } }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    response.year?.let { Text(it.toString(), color = KinoMuted) }
                    response.rating?.let { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = KinoGold, modifier = Modifier.size(16.dp)); Text("${it / 10.0}", color = KinoMuted) } }
                    Text(response.type.name.replace('_', ' '), color = KinoMuted)
                }
                Spacer(Modifier.height(16.dp)); Text(response.plot ?: "No synopsis available.", color = Color.White.copy(alpha = .84f), lineHeight = 22.sp)
                if (response.tags.isNotEmpty()) { Spacer(Modifier.height(14.dp)); Text(response.tags.joinToString("  •  "), color = KinoMuted, fontSize = 13.sp) }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamSelectorSheet(links: List<ExtractorLink>, loading: Boolean, onDismiss: () -> Unit, onSelect: (ExtractorLink) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = KinoSurface) {
        Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 28.dp)) {
            Text("Choose your stream", fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text("Select language and quality before continuing", color = KinoMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
            if (loading) LoadingState("Loading available streams")
            else if (links.isEmpty()) Text("No playable streams were found.", color = KinoMuted, modifier = Modifier.padding(vertical = 24.dp))
            else links.forEach { link ->
                val subtitle = listOf(link.type.name, link.quality?.takeIf { it > 0 }?.toString()?.plus("p")).filterNotNull().joinToString("  •  ")
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(KinoSurface2).clickable { onSelect(link) }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(KinoGold), contentAlignment = Alignment.Center) { Text("▶", color = KinoBlack, fontWeight = FontWeight.Black) }
                    Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(link.name, fontWeight = FontWeight.Bold); Text(subtitle.ifBlank { "MovieBox stream" }, color = KinoMuted, fontSize = 12.sp) }
                }
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
private fun PlayerScreen(response: LoadResponse, link: ExtractorLink, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { ContinueWatchingStore(context) }
    val saved = remember(response.url, response.apiName) { store.all().firstOrNull { it.url == response.url && it.apiName == response.apiName } }
    val player = remember(link.url) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(link.url)); prepare(); saved?.position?.takeIf { it > 0 }?.let { seekTo(it) }; playWhenReady = true } }
    fun saveProgress() {
        val duration = player.duration.takeIf { it > 0 } ?: saved?.duration ?: 0L
        store.save(WatchProgress(response.url, response.apiName, response.name, response.posterUrl, player.currentPosition, duration))
    }
    androidx.compose.runtime.DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (!isPlaying) saveProgress() }
            override fun onPlaybackStateChanged(playbackState: Int) { if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) saveProgress() }
        }
        player.addListener(listener)
        onDispose { saveProgress(); player.removeListener(listener); player.release() }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(top = 30.dp, start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Text(response.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxWidth().height(240.dp))
        Text(link.name, Modifier.padding(20.dp), color = KinoMuted)
    }
}
