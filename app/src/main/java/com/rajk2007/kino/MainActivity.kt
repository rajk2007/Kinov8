package com.rajk2007.kino

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.rajk2007.kino.core.ExtractorLink
import com.rajk2007.kino.core.HomeSection
import com.rajk2007.kino.core.LoadResponse
import com.rajk2007.kino.core.SearchResponse
import com.rajk2007.kino.data.DetailsViewModel
import com.rajk2007.kino.data.HomeViewModel
import com.rajk2007.kino.downloads.AppContextHolder
import com.rajk2007.kino.downloads.DownloadController
import kotlinx.coroutines.launch

private val KinoBlack = Color(0xFF080808)
private val KinoSurface = Color(0xFF121212)
private val KinoSurface2 = Color(0xFF1A1A1A)
private val KinoGold = Color(0xFFE7A86B)
private val KinoMuted = Color(0xFFA4A09C)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContextHolder.context = applicationContext
        setContent { KinoTheme { KinoApp() } }
    }
}

@Composable
private fun KinoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            background = KinoBlack, surface = KinoSurface, surfaceVariant = KinoSurface2,
            primary = KinoGold, onPrimary = KinoBlack, onBackground = Color.White,
            onSurface = Color.White, onSurfaceVariant = KinoMuted
        ), content = content
    )
}

@Composable
private fun KinoApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(onOpen = { item -> nav.navigate("details?url=${Uri.encode(item.url)}&apiName=${Uri.encode(item.apiName)}") }) }
        composable(
            "details?url={url}&apiName={apiName}",
            arguments = listOf(navArgument("url") { type = NavType.StringType }, navArgument("apiName") { type = NavType.StringType })
        ) { entry ->
            DetailsScreen(
                url = entry.arguments?.getString("url").orEmpty(),
                apiName = entry.arguments?.getString("apiName").orEmpty(),
                onBack = { nav.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(onOpen: (SearchResponse) -> Unit, vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val heroItems = state.sections.firstOrNull()?.items.orEmpty().take(5)
    Scaffold(containerColor = KinoBlack, modifier = Modifier.navigationBarsPadding()) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("KINO", color = KinoGold, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { vm.refresh() }) { Icon(Icons.Default.Refresh, "Refresh", tint = Color.White) }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it; vm.search(it) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    placeholder = { Text("Search movies and series", color = KinoMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = KinoGold) },
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(18.dp))
            }
            if (state.searchResults.isNotEmpty()) {
                item { SectionHeader("Search results") }
                item { MediaRow(state.searchResults, onOpen) }
            } else if (state.loading) {
                item { LoadingState() }
            } else if (state.error != null && state.sections.isEmpty()) {
                item { ErrorState(state.error ?: "", vm::refresh) }
            } else {
                if (heroItems.isNotEmpty()) item { HeroPager(heroItems, onOpen) }
                state.sections.forEach { section ->
                    item { SectionHeader(section.title) }
                    item { MediaRow(section.items, onOpen) }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, null, tint = KinoMuted)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroPager(items: List<SearchResponse>, onOpen: (SearchResponse) -> Unit) {
    val pager = rememberPagerState(pageCount = { items.size })
    HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(390.dp)) { page ->
        val item = items[page]
        Box(Modifier.fillMaxSize().padding(horizontal = 20.dp).clip(RoundedCornerShape(24.dp)).clickable { onOpen(item) }) {
            AsyncImage(item.posterUrl, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, KinoBlack.copy(alpha = .92f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                Text("FEATURED", color = KinoGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(item.name, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Text("Watch now  •  MovieBox", color = Color.White.copy(alpha = .78f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MediaRow(items: List<SearchResponse>, onOpen: (SearchResponse) -> Unit) {
    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { "${it.apiName}:${it.url}" }) { item -> MediaCard(item, onOpen) }
    }
}

@Composable
private fun MediaCard(item: SearchResponse, onOpen: (SearchResponse) -> Unit) {
    Column(Modifier.width(132.dp).clickable { onOpen(item) }) {
        AsyncImage(item.posterUrl, item.name, Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.height(8.dp))
        Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        Text(if (item.type == com.rajk2007.kino.core.MediaType.TV_SERIES) "Series" else "Movie", color = KinoMuted, fontSize = 12.sp)
    }
}

@Composable
private fun LoadingState() { Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = KinoGold) } }

@Composable
private fun ErrorState(message: String, retry: () -> Unit) { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Could not reach MovieBox", fontWeight = FontWeight.Bold); Text(message, color = KinoMuted, modifier = Modifier.padding(vertical = 8.dp)); TextButton(onClick = retry) { Text("Try again", color = KinoGold) } } }

@Composable
private fun DetailsScreen(url: String, apiName: String, onBack: () -> Unit, vm: DetailsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var playing by remember { mutableStateOf<ExtractorLink?>(null) }
    LaunchedEffect(url, apiName) { vm.load(url) }
    val response = state.response
    if (playing != null) {
        PlayerScreen(link = playing!!, title = response?.name ?: "KINO", onBack = { playing = null })
        return
    }
    Scaffold(containerColor = KinoBlack) { padding ->
        if (state.loading) LoadingState() else if (response == null) ErrorState(state.error ?: "Details unavailable", onBack) else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Box(Modifier.fillMaxWidth().height(410.dp)) {
                        AsyncImage(response.backgroundPosterUrl ?: response.posterUrl, response.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(KinoBlack.copy(alpha = .1f), KinoBlack))))
                        IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) { Icon(Icons.Default.ArrowBack, "Back") }
                        Text(response.name, Modifier.align(Alignment.BottomStart).padding(22.dp), fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = {
                                response.episodes.firstOrNull()?.let { vm.loadLinks(it.id) } ?: vm.loadLinks(response.data)
                            }, colors = ButtonDefaults.buttonColors(containerColor = KinoGold, contentColor = KinoBlack)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play", fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(10.dp))
                            Button(onClick = {
                                state.links.firstOrNull()?.let { DownloadController.enqueue(response.name, it) } ?: vm.loadLinks(response.episodes.firstOrNull()?.id ?: response.data)
                            }, colors = ButtonDefaults.buttonColors(containerColor = KinoSurface2, contentColor = Color.White)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Download") }
                        }
                        if (state.links.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp)); Text("Available streams", color = KinoGold, fontWeight = FontWeight.Bold)
                            state.links.forEach { link -> TextButton(onClick = { playing = link }) { Text(link.name, color = Color.White) } }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            response.year?.let { Text(it.toString(), color = KinoMuted) }
                            response.rating?.let { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = KinoGold, modifier = Modifier.size(16.dp)); Text("${it / 10.0}", color = KinoMuted) } }
                            Text(response.type.name.replace('_', ' '), color = KinoMuted)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(response.plot ?: "No synopsis available.", color = Color.White.copy(alpha = .82f), lineHeight = 22.sp)
                        if (response.tags.isNotEmpty()) { Spacer(Modifier.height(14.dp)); Text(response.tags.joinToString("  •  "), color = KinoMuted, fontSize = 13.sp) }
                        Spacer(Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(link: ExtractorLink, title: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(link.url) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(link.url)); prepare(); playWhenReady = true } }
    androidx.compose.runtime.DisposableEffect(player) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(top = 30.dp, start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Text(title, fontWeight = FontWeight.Bold) }
        AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxWidth().height(240.dp))
        Text(link.name, Modifier.padding(20.dp), color = KinoMuted)
    }
}

