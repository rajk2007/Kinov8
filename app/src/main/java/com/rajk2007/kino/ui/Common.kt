package com.rajk2007.kino.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rajk2007.kino.core.MediaType
import com.rajk2007.kino.core.SearchResponse

@Composable
fun SectionHeader(title: String, action: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        if (action != null) Icon(Icons.Default.KeyboardArrowRight, null, tint = KinoMuted, modifier = Modifier.clickable { action() })
    }
}

@Composable
fun MediaRow(items: List<SearchResponse>, onOpen: (SearchResponse) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { "${it.apiName}:${it.url}" }) { item -> MediaCard(item, onOpen) }
    }
}

@Composable
fun MediaCard(item: SearchResponse, onOpen: (SearchResponse) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.width(132.dp).clickable { onOpen(item) }) {
        Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(12.dp)).background(KinoSurface2)) {
            AsyncImage(item.posterUrl, item.name, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(8.dp))
        Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
        Text(if (item.type == MediaType.TV_SERIES) "Series" else "Movie", color = KinoMuted, fontSize = 12.sp)
    }
}

@Composable
fun LoadingState(label: String = "Loading MovieBox") {
    Column(Modifier.fillMaxWidth().padding(vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = KinoGold, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(14.dp)); Text(label, color = KinoMuted)
    }
}

@Composable
fun ErrorState(message: String, retry: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Something went wrong", fontWeight = FontWeight.Bold)
        Text(message, color = KinoMuted, modifier = Modifier.padding(vertical = 8.dp))
        retry?.let { TextButton(onClick = it) { Text("Try again", color = KinoGold) } }
    }
}
