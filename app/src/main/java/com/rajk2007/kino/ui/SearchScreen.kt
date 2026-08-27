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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rajk2007.kino.core.SearchResponse
import com.rajk2007.kino.data.SearchViewModel

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (SearchResponse) -> Unit, vm: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = KinoBlack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Column {
                    Text("Discover", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("Powered directly by MovieBox", color = KinoMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f)); Icon(Icons.Default.Tune, "Filters", tint = KinoGold)
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChanged,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = { Text("Search movies, series, anime...", color = KinoMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = KinoGold) },
                shape = RoundedCornerShape(18.dp)
            )
            when {
                state.loading -> LoadingState("Searching MovieBox")
                state.error != null -> ErrorState(state.error ?: "Search failed", vm::retry)
                !state.hasSearched -> EmptySearchState()
                state.results.isEmpty() -> EmptyResultsState(state.query)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    item { Text("${state.results.size} results", color = KinoMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp)) }
                    items(state.results, key = { "${it.apiName}:${it.url}" }) { result -> SearchResultRow(result, onOpen) }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: SearchResponse, onOpen: (SearchResponse) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(KinoSurface).clickable { onOpen(item) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(item.posterUrl, item.name, Modifier.size(76.dp, 104.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp)); Text(if (item.type.name == "TV_SERIES") "Series" else "Movie", color = KinoGold, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp)); Text("MovieBox  •  Tap for details", color = KinoMuted, fontSize = 12.sp)
        }
    }
}

@Composable private fun EmptySearchState() { Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) { Text("Search the MovieBox catalog", color = KinoMuted) } }
@Composable private fun EmptyResultsState(query: String) { Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) { Text("No results for “$query”", color = KinoMuted) } }
