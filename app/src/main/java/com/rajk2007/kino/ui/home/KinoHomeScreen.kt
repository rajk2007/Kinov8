package com.rajk2007.kino.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rajk2007.kino.core.HomeSection
import com.rajk2007.kino.core.MediaType
import com.rajk2007.kino.core.SearchResponse
import com.rajk2007.kino.ui.ErrorState
import com.rajk2007.kino.ui.KinoBlack
import com.rajk2007.kino.ui.KinoGold
import com.rajk2007.kino.ui.KinoMuted
import com.rajk2007.kino.ui.KinoSurface
import com.rajk2007.kino.ui.KinoSurface2
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KinoHomeScreen(
    sections: List<HomeSection>,
    isLoading: Boolean,
    error: String?,
    onMovieClick: (SearchResponse) -> Unit,
    onSearchClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val categories = listOf("All", "Movies", "Series", "Anime", "Hindi")
    var selectedCategory by remember { mutableStateOf("All") }
    val visibleSections = when (selectedCategory) {
        "Movies" -> sections.filterNot { it.title.equals("Series", true) }
        "Series" -> sections.filter { it.title.contains("series", true) || it.title.contains("tv", true) }
        else -> sections
    }
    val heroItems = sections.firstOrNull()?.items.orEmpty().take(7)
    val pagerState = rememberPagerState(pageCount = { heroItems.size.coerceAtLeast(1) })

    LaunchedEffect(heroItems.size) {
        if (heroItems.size > 1) while (true) {
            delay(5000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % heroItems.size)
        }
    }

    Box(Modifier.fillMaxSize().background(KinoBlack)) {
        if (isLoading && sections.isEmpty()) ShimmerLoading()
        else LazyColumn(contentPadding = PaddingValues(bottom = 60.dp), modifier = Modifier.fillMaxSize()) {
            item { Header(onSearchClick, onLibraryClick, onRefresh) }
            item { QuickDiscoveryChips(categories, selectedCategory) { selectedCategory = it } }
            if (error != null && sections.isEmpty()) item { ErrorState(error, onRefresh) }
            else {
                if (selectedCategory == "All" && heroItems.isNotEmpty()) item { HeroBanner(heroItems, pagerState, onMovieClick) }
                visibleSections.forEach { section ->
                    item { MovieSection(section.title, section.items, onMovieClick) }
                }
            }
        }
    }
}

@Composable
private fun Header(onSearch: () -> Unit, onLibrary: () -> Unit, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("KINO", color = KinoGold, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("Cinema, curated for you", color = KinoMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search", tint = Color.White) }
        IconButton(onClick = onLibrary) { Icon(Icons.Default.LibraryBooks, "Library", tint = Color.White) }
        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh", tint = Color.White) }
        IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "Notifications", tint = Color.White) }
    }
}

@Composable
private fun QuickDiscoveryChips(categories: List<String>, selected: String, onSelected: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { category ->
            val active = selected == category
            Box(Modifier.clip(RoundedCornerShape(50)).background(if (active) KinoGold else KinoSurface2).border(1.dp, if (active) KinoGold else Color.White.copy(alpha = .08f), RoundedCornerShape(50)).clickable { onSelected(category) }.padding(horizontal = 17.dp, vertical = 9.dp)) {
                Text(category, color = if (active) KinoBlack else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroBanner(items: List<SearchResponse>, pagerState: PagerState, onMovieClick: (SearchResponse) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 18.dp).height(360.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val movie = items[page % items.size]
            Box(Modifier.fillMaxSize().padding(horizontal = 18.dp).clip(RoundedCornerShape(22.dp)).clickable { onMovieClick(movie) }) {
                AsyncImage(movie.posterUrl, movie.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, KinoBlack.copy(alpha = .96f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                    Text("FEATURED  •  MOVIEBOX", color = KinoGold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                    Text(movie.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp)); Text("Tap to explore details", color = Color.White.copy(alpha = .76f), fontSize = 13.sp)
                }
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
            repeat(items.size) { index -> Box(Modifier.padding(horizontal = 3.dp).size(if (pagerState.currentPage == index) 20.dp else 7.dp, 7.dp).clip(CircleShape).background(if (pagerState.currentPage == index) KinoGold else Color.White.copy(alpha = .45f))) }
        }
    }
}

@Composable
private fun MovieSection(title: String, items: List<SearchResponse>, onMovieClick: (SearchResponse) -> Unit) {
    if (items.isEmpty()) return
    Column(Modifier.padding(top = 19.dp)) {
        Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 18.dp, bottom = 10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            items(items, key = { "${it.apiName}:${it.url}" }) { movie -> MovieCard(movie, onMovieClick) }
        }
    }
}

@Composable
private fun MovieCard(movie: SearchResponse, onClick: (SearchResponse) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) .94f else 1f, label = "poster-scale")
    Column(Modifier.width(112.dp).scale(scale).clickable(interactionSource = interaction, indication = null) { onClick(movie) }) {
        Box(Modifier.fillMaxWidth().height(164.dp).clip(RoundedCornerShape(10.dp)).background(KinoSurface)) { AsyncImage(movie.posterUrl, movie.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Text(movie.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
        Text(if (movie.type == MediaType.TV_SERIES) "Series" else "Movie", color = KinoMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp))
    }
}

@Composable
private fun ShimmerLoading() {
    val transition = rememberInfiniteTransition(label = "kino-shimmer")
    val offset by transition.animateFloat(0f, 1200f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "shimmer-offset")
    val brush = Brush.linearGradient(listOf(KinoSurface, KinoSurface2, KinoSurface), start = androidx.compose.ui.geometry.Offset(offset, offset), end = androidx.compose.ui.geometry.Offset(offset + 300, offset + 300))
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Spacer(Modifier.height(70.dp)); Box(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(22.dp)).background(brush)); Spacer(Modifier.height(24.dp))
        repeat(2) { Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) { repeat(3) { Box(Modifier.size(112.dp, 164.dp).clip(RoundedCornerShape(10.dp)).background(brush)) } } ; Spacer(Modifier.height(28.dp)) }
    }
}
