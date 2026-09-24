package com.mport.tv.feature.history

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
import com.mport.tv.data.local.WatchHistoryEntity
import kotlinx.coroutines.flow.map

@Composable
fun HistoryScreen(onPlayer: (String) -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as MPorTApplication
    val history by app.database.watchHistoryDao().observeAll()
        .collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Watch History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (history.isEmpty()) {
            Text(
                "Belum ada riwayat tontonan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(history, key = { it.channelId }) { item: WatchHistoryEntity ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayer(item.channelId) }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(item.channelName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Last watched: ${item.lastWatchedEpochMs}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
