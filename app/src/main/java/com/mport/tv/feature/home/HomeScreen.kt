package com.mport.tv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mport.tv.MPorTApplication
import com.mport.tv.core.model.Channel
import com.mport.tv.data.playlist.BuiltInPlaylist
import com.mport.tv.data.playlist.DemoPlaylist
import com.mport.tv.data.playlist.M3UParser
import com.mport.tv.data.playlist.PlaylistImporter
import com.mport.tv.data.repository.ChannelRepositoryImpl
import com.mport.tv.data.settings.AppSettings
import com.mport.tv.ui.theme.CyanAccent
import com.mport.tv.ui.theme.NavyCard
import com.mport.tv.ui.theme.PowerRed
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onEpg: () -> Unit = {},
    onPlayer: (String) -> Unit = {},
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
    var selectedGroup by remember { mutableStateOf<String?>(null) }

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
                    val builtIn = BuiltInPlaylist.load(context)
                    if (builtIn.isNotEmpty()) {
                        repo.replaceChannels(builtIn)
                        message = "Playlist MPorT loaded (${builtIn.size} channels)"
                    } else {
                        repo.replaceChannels(DemoPlaylist.channels())
                        message = "Demo streams loaded"
                    }
                }
            } catch (e: Exception) {
                try {
                    val builtIn = BuiltInPlaylist.load(context)
                    if (builtIn.isNotEmpty()) {
                        repo.replaceChannels(builtIn)
                        message = "Playlist MPorT loaded (${builtIn.size})"
                    } else {
                        repo.replaceChannels(DemoPlaylist.channels())
                        message = "Demo (error: ${e.message})"
                    }
                } catch (e2: Exception) {
                    repo.replaceChannels(DemoPlaylist.channels())
                    message = "Demo (error: ${e.message})"
                }
            } finally {
                loading = false
            }
        }
    }

    val groups = remember(channels) {
        channels.mapNotNull { it.groupTitle }.distinct().sorted()
    }
    val filtered = remember(channels, selectedGroup) {
        if (selectedGroup == null) channels
        else channels.filter { it.groupTitle == selectedGroup }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MPorT",
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(
                text = " TV",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "Indonesia",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PowerRed)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text("⏻", color = Color.White, fontSize = 18.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            TextButton(onClick = onSearch) { Text("Search") }
            TextButton(onClick = onFavorites) { Text("Fav") }
            TextButton(onClick = onHistory) { Text("History") }
            TextButton(onClick = onEpg) { Text("EPG") }
            TextButton(onClick = onYoutube) { Text("YouTube") }
            TextButton(onClick = onSettings) { Text("Settings") }
            TextButton(onClick = onAbout) { Text("About") }
        }

        if (groups.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedGroup == null,
                    onClick = { selectedGroup = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                groups.forEach { g ->
                    FilterChip(
                        selected = selectedGroup == g,
                        onClick = { selectedGroup = g },
                        label = { Text(g, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = NavyCard
                        )
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CyanAccent)
            }
            filtered.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Belum ada channel", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Buka Settings untuk load playlist M3U",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onSettings) { Text("Settings") }
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { it.id }) { ch ->
                        ChannelRow(
                            channel = ch,
                            onClick = { onPlayer(ch.id) },
                            onToggleFavorite = {
                                scope.launch {
                                    repo.setFavorite(ch.id, !ch.isFavorite)
                                }
                            }
                        )
                    }
                }
            }
        }

        message?.let {
            Text(
                it,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = onAbout,
                shape = RoundedCornerShape(24.dp),
                color = CyanAccent,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Join our community",
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            Surface(
                onClick = onSettings,
                shape = RoundedCornerShape(24.dp),
                color = CyanAccent,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Settings",
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    channel.name.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                channel.groupTitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
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
