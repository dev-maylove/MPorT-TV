package com.mport.tv.feature.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mport.tv.MPorTApplication
import com.mport.tv.data.repository.ChannelRepositoryImpl

@Composable
fun FavoritesScreen(onPlayer: (String) -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as MPorTApplication
    val repo = remember { ChannelRepositoryImpl(app.database) }
    val channels by repo.observeChannels().collectAsState(initial = emptyList())
    val favorites = channels.filter { it.isFavorite }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Favorites", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (favorites.isEmpty()) {
            Text(
                "Belum ada channel favorit.\nTandai ★ di daftar channel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(favorites, key = { it.id }) { ch ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayer(ch.id) }
                    ) {
                        Text(
                            ch.name,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
