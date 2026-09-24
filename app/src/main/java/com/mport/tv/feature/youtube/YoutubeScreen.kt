package com.mport.tv.feature.youtube

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mport.tv.core.model.Channel
import com.mport.tv.core.model.StreamType
import com.mport.tv.data.youtube.YoutubeIdParser
import com.mport.tv.data.youtube.YoutubeInnerTubeClient
import com.mport.tv.data.youtube.YoutubeStreamResolver
import androidx.compose.ui.platform.LocalContext
import com.mport.tv.data.youtube.YoutubeNsigNotes
import com.mport.tv.data.youtube.YoutubeSearchItem
import kotlinx.coroutines.launch

@Composable
fun YoutubeScreen(
    onPlayChannel: (Channel) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val client = remember { YoutubeInnerTubeClient() }
    val resolver = remember { YoutubeStreamResolver(context) }
    val scope = rememberCoroutineScope()

    var input by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<YoutubeSearchItem>>(emptyList()) }

    fun playVideoId(videoId: String) {
        scope.launch {
            loading = true
            status = "Resolving $videoId (InnerTube + nsig)…"
            try {
                val playable = resolver.resolvePlayable(videoId)
                val url = playable.bestDirectUrl()
                    ?: throw IllegalStateException(
                        "Tidak ada URL stream. ${YoutubeNsigNotes.MESSAGE}"
                    )
                val type = when {
                    url.contains(".m3u8", ignoreCase = true) -> StreamType.HLS
                    url.contains(".mpd", ignoreCase = true) -> StreamType.DASH
                    else -> StreamType.MP4
                }
                val channel = Channel(
                    id = "yt-${playable.videoId}",
                    name = playable.title,
                    logoUrl = playable.thumbnailUrl,
                    groupTitle = "YouTube",
                    streamUrl = url,
                    streamType = type,
                    epgId = playable.videoId
                )
                val note = if (playable.needsNsig()) {
                    " (cipher formats present — using best available URL)"
                } else ""
                status = "OK: ${playable.title}$note"
                onPlayChannel(channel)
            } catch (e: Exception) {
                status = "Error: ${e.message}"
            } finally {
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("YouTube", style = MaterialTheme.typography.headlineMedium)
        Text(
            "InnerTube player (opsi 1–2). nsig/PO penuh: ${YoutubeNsigNotes.STATUS}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Video URL atau ID (11 karakter)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://youtu.be/… atau dQw4w9WgXcQ") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val id = YoutubeIdParser.extract(input)
                    if (id == null) status = "Video ID tidak valid"
                    else playVideoId(id)
                },
                enabled = !loading
            ) { Text(if (loading) "Loading…" else "Play") }
            OutlinedButton(onClick = onBack) { Text("Kembali") }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Cari YouTube") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    status = null
                    try {
                        results = client.search(searchQuery.trim()).getOrThrow()
                        status = "${results.size} hasil"
                    } catch (e: Exception) {
                        status = "Search error: ${e.message}"
                        results = emptyList()
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading && searchQuery.isNotBlank()
        ) { Text("Search") }

        status?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(results, key = { it.videoId }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !loading) { playVideoId(item.videoId) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge)
                        item.author?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(item.videoId, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
