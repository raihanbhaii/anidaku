package com.anilite

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.anilite.ui.screens.AnimeDetailScreen
import com.anilite.ui.screens.HomeScreen
import com.anilite.ui.screens.SearchScreen
import com.anilite.ui.screens.WatchlistScreen
import com.anilite.ui.theme.AnidakuTheme
import com.anilite.ui.theme.Purple40
import com.anilite.ui.theme.SurfaceVariant

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AnidakuTheme {
                AnidakuApp(
                    onPlayEpisode = { episodeId: String, episodeTitle: String ->
                        startActivity(
                            Intent(this@MainActivity, PlayerActivity::class.java).apply {
                                putExtra("episodeId", episodeId)
                                putExtra("episodeTitle", episodeTitle)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun AnidakuApp(
    onPlayEpisode: (String, String) -> Unit   // This expects (episodeId, fullTitle)
) {
    val navController = rememberNavController()

    val bottomItems = listOf(
        Triple("home", "Home", Icons.Default.Home),
        Triple("search", "Search", Icons.Default.Search),
        Triple("watchlist", "Watchlist", Icons.Default.Bookmark)
    )

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute?.startsWith("detail") == false) {
                NavigationBar(
                    containerColor = SurfaceVariant,
                    tonalElevation = androidx.compose.ui.unit.Dp.Hairline
                ) {
                    bottomItems.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Purple40,
                                selectedTextColor = Purple40,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    onAnimeClick = { animeId ->
                        navController.navigate("detail/$animeId")
                    }
                )
            }

            composable("search") {
                SearchScreen(
                    onAnimeClick = { animeId ->
                        navController.navigate("detail/$animeId")
                    }
                )
            }

            composable("watchlist") {
                WatchlistScreen(
                    onAnimeClick = { animeId ->
                        navController.navigate("detail/$animeId")
                    }
                )
            }

            composable(
                route = "detail/{animeId}",
                arguments = listOf(
                    navArgument("animeId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val animeId = backStackEntry.arguments?.getInt("animeId") ?: -1

                AnimeDetailScreen(
                    animeId = animeId,
                    onBack = { navController.popBackStack() },
                    onPlayEpisode = { episodeId: String, episodeTitle: String, episodeNumber: Int ->
                        // Properly combine title and episode number into one String
                        val fullTitle = "$episodeTitle - Episode $episodeNumber"
                        onPlayEpisode(episodeId, fullTitle)
                    }
                )
            }
        }
    }
}
