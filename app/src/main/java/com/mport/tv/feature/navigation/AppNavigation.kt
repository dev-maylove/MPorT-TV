package com.mport.tv.feature.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mport.tv.feature.about.AboutScreen
import com.mport.tv.feature.epg.EpgScreen
import com.mport.tv.feature.favorites.FavoritesScreen
import com.mport.tv.feature.history.HistoryScreen
import com.mport.tv.feature.home.HomeScreen
import com.mport.tv.feature.legal.LicenseScreen
import com.mport.tv.feature.legal.PrivacyScreen
import com.mport.tv.feature.player.PlayerScreen
import com.mport.tv.feature.search.SearchScreen
import com.mport.tv.feature.settings.SettingsScreen
import com.mport.tv.feature.youtube.YoutubeScreen
import com.mport.tv.MPorTApplication
import com.mport.tv.data.repository.ChannelRepositoryImpl
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

object Routes {
    const val HOME = "home"
    const val EPG = "epg"
    const val PLAYER = "player/{channelId}"
    const val SETTINGS = "settings"
    const val FAVORITES = "favorites"
    const val HISTORY = "history"
    const val SEARCH = "search"
    const val ABOUT = "about"
    const val LICENSE = "license"
    const val PRIVACY = "privacy"
    const val YOUTUBE = "youtube"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onEpg = { nav.navigate(Routes.EPG) },
                onPlayer = { id -> nav.navigate("player/$id") },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onFavorites = { nav.navigate(Routes.FAVORITES) },
                onHistory = { nav.navigate(Routes.HISTORY) },
                onSearch = { nav.navigate(Routes.SEARCH) },
                onAbout = { nav.navigate(Routes.ABOUT) },
                onYoutube = { nav.navigate(Routes.YOUTUBE) }
            )
        }
        composable(Routes.EPG) { EpgScreen() }
        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
        ) { backStack ->
            val id = backStack.arguments?.getString("channelId")
            if (id == null) Text("Channel tidak ditemukan")
            else PlayerScreen(channelId = id)
        }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.FAVORITES) {
            FavoritesScreen(onPlayer = { id -> nav.navigate("player/$id") })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(onPlayer = { id -> nav.navigate("player/$id") })
        }
        composable(Routes.SEARCH) {
            SearchScreen(onPlayer = { id -> nav.navigate("player/$id") })
        }
        composable(Routes.ABOUT) {
            AboutScreen(
                onLicense = { nav.navigate(Routes.LICENSE) },
                onPrivacy = { nav.navigate(Routes.PRIVACY) }
            )
        }
        composable(Routes.LICENSE) { LicenseScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PRIVACY) { PrivacyScreen(onBack = { nav.popBackStack() }) }

        composable(Routes.YOUTUBE) {
            val context = LocalContext.current
            val app = context.applicationContext as MPorTApplication
            val repo = ChannelRepositoryImpl(app.database)
            YoutubeScreen(
                onPlayChannel = { channel ->
                    CoroutineScope(Dispatchers.Main).launch {
                        // Merge into local list so PlayerScreen can find by id
                        val current = repo.observeChannels().first()
                        val merged = listOf(channel) + current.filter { it.id != channel.id }
                        repo.replaceChannels(merged)
                        nav.navigate("player/${channel.id}")
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }

    }
}
