package com.mport.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mport.tv.feature.navigation.AppNavigation
import com.mport.tv.feature.splash.AnimatedSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MPorTTV)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        AnimatedSplashScreen(
                            onFinished = { showSplash = false }
                        )
                    } else {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn()
                        ) {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }
}
