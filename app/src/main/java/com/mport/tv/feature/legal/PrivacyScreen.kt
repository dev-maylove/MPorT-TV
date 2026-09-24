package com.mport.tv.feature.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyScreen(onBack: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Privacy Policy", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            """
            Last updated: September 2026

            1. Data we store locally
            • Playlist URL you enter in Settings (DataStore)
            • Channel list, favorites, and watch history (Room database)
            All of the above stays on your device only.

            2. Network
            • The app only contacts servers that you configure (playlist URL, stream URLs, EPG URL).
            • No analytics, advertising, or third-party tracking SDKs are included in this clean-room build.
            • HTTPS is enforced (cleartext traffic disabled).

            3. Permissions
            • INTERNET — to load playlists and streams you request
            • ACCESS_NETWORK_STATE — to check connectivity
            • WAKE_LOCK — to keep playback alive

            4. Third-party content
            You are solely responsible for ensuring you have the right to access any playlist or stream you load.

            5. Children
            This app is not directed at children under 13.

            6. Changes
            This policy may be updated; the in-app text is the current version.

            7. Contact
            For privacy questions about this open-source project, use the project repository or maintainer channel.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Kembali") }
    }
}
