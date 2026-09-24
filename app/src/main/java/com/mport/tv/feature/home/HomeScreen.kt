package com.mport.tv.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mport.tv.MPorTApplication
import com.mport.tv.core.model.Channel
import com.mport.tv.data.playlist.DemoPlaylist
import com.mport.tv.data.playlist.M3UParser
import com.mport.tv.data.playlist.PlaylistImporter
import com.mport.tv.data.repository.ChannelRepositoryImpl
import com.mport.tv.data.settings.AppSettings
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onEpg: () -> Unit,
    onPlayer: (String) -> Unit,
    onSettings: () -> Unit = {},
    onFavorites: () -> Unit = {},
    onHistory: () -> Unit = {},
    onSearch: () -> Unit = {},
    onAbout: () -> Unit = {},
    onYoutube: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as MPorTApplication
    val repo = remember { ChannelRepositoryImpl(app.database) }
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()

    val channels by repo.observeChannels().collectAsState(initial = emptyList())
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Auto-load: user playlist URL, else demo samples
    LaunchedEffect(Unit) {
        settings.playlistUrl.collect { url ->
            if (channels.isNotEmpty()) return@collect
            loading = true
            message = null
            try {
                if (!url.isNullOrBlank()) {
                    val body = PlaylistImporter.download(url)
                    val parsed = M3UParser.parse(body)
                    if (parsed.isNotEmpty()) {
                        repo.replaceChannels(parsed)
                        message = "Loaded ${parsed.size} channels"
                    } else {
                        message = "Playlist kosong"
                    }
                } else {
                    // Built-in public demo streams (not a private IPTV list)
                    repo.replaceChannels(DemoPlaylist.channels())
                    message = "Demo streams loaded (${DemoPlaylist.channels().size})"
                }
            } catch (e: Exception) {
                // fallback demo
                repo.replaceChannels(DemoPlaylist.channels())
                message = "Demo streams (playlist gagal: ${e.message})"
            } finally {
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("MPorT TV", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${channels.size} channels",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onSearch) { Text("Search") }
                TextButton(onClick = onFavorites) { Text("Fav") }
                TextButton(onClick = onHistory) { Text("History") }
                TextButton(onClick = onEpg) { Text("EPG") }
                TextButton(onClick = onSettings) { Text("Settings") }
                TextButton(onClick = onYoutube) { Text("YouTube") }
                TextButton(onClick = onAbout) { Text("About") }
            }
        }

        HorizontalDivider()

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (channels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Belum ada channel",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Buka Settings → masukkan URL playlist M3U milik Anda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onSettings) { Text("Buka Settings") }
                }
            }
        } else {
            // Group by groupTitle
            val grouped = channels.groupBy { it.groupTitle ?: "Lainnya" }

            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (group, list) ->
                    item {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                    items(list, key = { it.id }) { channel ->
                        ChannelRow(
                            channel = channel,
                            onClick = { onPlayer(channel.id) },
                            onToggleFavorite = {
                                scope.launch {
                                    repo.setFavorite(channel.id, !channel.isFavorite)
                                }
                            }
                        )
                    }
                }
            }
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!channel.groupTitle.isNullOrBlank()) {
                    Text(
                        text = channel.groupTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onToggleFavorite) {
                Text(if (channel.isFavorite) "★" else "☆")
            }
        }
    }
}
