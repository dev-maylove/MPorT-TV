package com.mport.tv.feature.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mport.tv.R

@Composable
fun AboutScreen(
    onLicense: () -> Unit = {},
    onPrivacy: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = "Logo",
            modifier = Modifier.size(96.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("MPorT TV", style = MaterialTheme.typography.headlineMedium)
        Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Clean-room Android IPTV / media player.\n" +
                "Supports M3U/M3U8, HLS, DASH, MP4 via Media3.
YouTube InnerTube + nsigsolver/potokennp2 assets (WebView).\n" +
                "Favorites, history, search, EPG (XMLTV), secure update primitives.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "This project does not ship proprietary credentials, private endpoints, " +
                "or content from third-party APKs. Use only sources you own or are authorized to use.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onLicense) { Text("License") }
            OutlinedButton(onClick = onPrivacy) { Text("Privacy Policy") }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Demo streams included are public test HLS samples only (Sintel, Tears of Steel, Mux, Apple demos).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
