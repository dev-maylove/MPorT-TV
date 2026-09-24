package com.mport.tv.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mport.tv.MPorTApplication
import com.mport.tv.data.playlist.DemoPlaylist
import com.mport.tv.data.playlist.M3UParser
import com.mport.tv.data.playlist.PlaylistImporter
import com.mport.tv.data.repository.ChannelRepositoryImpl
import com.mport.tv.data.settings.AppSettings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as MPorTApplication
    val repo = remember { ChannelRepositoryImpl(app.database) }
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()

    val savedUrl by settings.playlistUrl.collectAsState(initial = null)
    var urlInput by remember { mutableStateOf(savedUrl ?: "") }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(savedUrl) {
        if (urlInput.isBlank() && !savedUrl.isNullOrBlank()) {
            urlInput = savedUrl!!
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Playlist M3U URL") },
            placeholder = { Text("https://example.com/playlist.m3u") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        status = null
                        try {
                            settings.setPlaylistUrl(urlInput.trim())
                            if (urlInput.isNotBlank()) {
                                val text = PlaylistImporter.download(urlInput.trim())
                                val parsed = M3UParser.parse(text)
                                repo.replaceChannels(parsed)
                                status = "Berhasil: ${parsed.size} channel dimuat"
                            } else {
                                status = "URL dikosongkan"
                            }
                        } catch (e: Exception) {
                            status = "Error: ${e.message}"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading
            ) {
                Text(if (loading) "Loading..." else "Simpan & Load Playlist")
            }
            OutlinedButton(onClick = onBack) { Text("Kembali") }
        }
        OutlinedButton(
            onClick = {
                scope.launch {
                    loading = true
                    try {
                        repo.replaceChannels(DemoPlaylist.channels())
                        status = "Demo streams loaded (${DemoPlaylist.channels().size})"
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading
        ) { Text("Load Demo Streams") }

        status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            "Catatan:\n" +
                "• Hanya gunakan playlist/API yang Anda miliki atau berwenang.\n" +
                "• Format yang didukung: M3U / M3U8.\n" +
                "• Stream HLS, DASH, dan MP4 didukung via Media3.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
