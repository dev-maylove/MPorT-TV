package com.mport.tv.feature.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.PlayerView
import com.mport.tv.MPorTApplication
import com.mport.tv.core.model.Channel
import com.mport.tv.data.player.PlayerManager
import com.mport.tv.data.local.WatchHistoryEntity
import com.mport.tv.data.repository.ChannelRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(channelId: String) {
    val context = LocalContext.current
    val app = context.applicationContext as MPorTApplication
    val repo = remember { ChannelRepositoryImpl(app.database) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var channel by remember { mutableStateOf<Channel?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    val playerManager = remember { PlayerManager(context) }

    // Load channel
    LaunchedEffect(channelId) {
        loading = true
        error = null
        try {
            val channels = repo.observeChannels().first()
            val found = channels.find { it.id == channelId }
            if (found == null) {
                error = "Channel tidak ditemukan"
            } else {
                channel = found
                playerManager.play(found)
                // Record history
                app.database.watchHistoryDao().upsert(
                    WatchHistoryEntity(
                        channelId = found.id,
                        channelName = found.name,
                        lastWatchedEpochMs = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            error = e.message ?: "Gagal memuat channel"
        } finally {
            loading = false
        }
    }

    // Lifecycle: pause/resume/release
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> playerManager.player.playWhenReady = false
                Lifecycle.Event.ON_RESUME -> playerManager.player.playWhenReady = true
                Lifecycle.Event.ON_DESTROY -> playerManager.release()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            playerManager.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            error != null -> {
                Text(
                    text = error ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            }
            else -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            player = playerManager.player
                            useController = true
                        }
                    },
                    update = { view ->
                        view.player = playerManager.player
                    }
                )
                // Overlay channel name
                channel?.let { ch ->
                    Text(
                        text = ch.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
