package com.mport.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mport.tv.data.settings.AppSettings
import com.mport.tv.feature.navigation.AppNavigation
import com.mport.tv.feature.splash.AnimatedSplashScreen
import com.mport.tv.ui.theme.MPorTTheme
import com.mport.tv.ui.theme.rememberAppDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setTheme(R.style.Theme_MPorTTV)
        val settings = AppSettings(applicationContext)

        setContent {
            val darkPref by settings.darkMode.collectAsState(initial = true)
            val darkTheme = rememberAppDarkTheme(darkPref)

            MPorTTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }
                    if (showSplash) {
                        AnimatedSplashScreen(onFinished = { showSplash = false })
                    } else {
                        AnimatedVisibility(visible = true, enter = fadeIn()) {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }
}
