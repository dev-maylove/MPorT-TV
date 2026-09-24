package com.mport.tv.feature.search

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
fun SearchScreen(onPlayer: (String) -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as MPorTApplication
    val repo = remember { ChannelRepositoryImpl(app.database) }
    val channels by repo.observeChannels().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, channels) {
        if (query.isBlank()) emptyList()
        else channels.filter {
            it.name.contains(query, ignoreCase = true) ||
                (it.groupTitle?.contains(query, ignoreCase = true) == true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Search", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cari channel") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        if (query.isBlank()) {
            Text(
                "Ketik nama channel atau grup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (filtered.isEmpty()) {
            Text("Tidak ditemukan.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.id }) { ch ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayer(ch.id) }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(ch.name, style = MaterialTheme.typography.bodyLarge)
                            ch.groupTitle?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
