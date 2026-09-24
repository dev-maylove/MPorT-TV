package com.mport.tv.feature.epg

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EpgScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("EPG", style = MaterialTheme.typography.headlineMedium)
        Text("XMLTV/API EPG layer siap dikonfigurasi.")
    }
}
