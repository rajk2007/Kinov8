package com.rajk2007.kino.ui

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import coil.compose.AsyncImage
import com.rajk2007.kino.core.SearchResponse
import com.rajk2007.kino.data.HomeViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(onOpen: (SearchResponse) -> Unit, onSearch: () -> Unit, onLibrary: () -> Unit, vm: HomeViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val heroItems = state.sections.firstOrNull()?.items.orEmpty().take(5)
    Scaffold(containerColor = KinoBlack, modifier = Modifier.navigationBarsPadding()) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("KINO", color = KinoGold, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                        Text("Your cinema, curated", color = KinoMuted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search", tint = Color.White) }
                    IconButton(onClick = onLibrary) { Icon(Icons.Default.LibraryBooks, "Library", tint = Color.White) }
                    IconButton(onClick = vm::refresh) { Icon(Icons.Default.Refresh, "Refresh", tint = Color.White) }
                }
            }
            when {
                state.loading -> item { LoadingState() }
                state.error != null && state.sections.isEmpty() -> item { ErrorState(state.error ?: "MovieBox unavailable", vm::refresh) }
                else -> {
                    if (heroItems.isNotEmpty()) item { HeroPager(heroItems, onOpen) }
                    state.sections.forEach { section ->
                        item { SectionHeader(section.title) }
                        item { MediaRow(section.items, onOpen) }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
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
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, KinoBlack.copy(alpha = .94f)))))
            Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                Text("FEATURED  •  MOVIEBOX", color = KinoGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                Text(item.name, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp)); Text("Tap to see details", color = Color.White.copy(alpha = .75f), fontSize = 13.sp)
            }
        }
    }
}
