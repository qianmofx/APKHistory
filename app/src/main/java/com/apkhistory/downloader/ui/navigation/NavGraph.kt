package com.apkhistory.downloader.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.apkhistory.downloader.ui.detail.DetailScreen
import com.apkhistory.downloader.ui.downloads.DownloadsScreen
import com.apkhistory.downloader.ui.downloads.DownloadsViewModel
import com.apkhistory.downloader.ui.downloads.FavoritesScreen
import com.apkhistory.downloader.ui.search.SearchScreen
import com.apkhistory.downloader.ui.versiondetail.VersionDetailScreen
import com.apkhistory.downloader.ui.versions.VersionsScreen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/** 下载参数 */
data class DownloadParams(
    val appId: String,
    val appName: String,
    val packageName: String,
    val vid: String,
    val versionName: String,
    val iconUrl: String,
    val apkSize: String = ""
)

private val bottomNavItems = listOf(
    BottomNavItem("搜索", Icons.Default.Search, Screen.Search.route),
    BottomNavItem("下载", Icons.Default.Download, Screen.Downloads.route),
    BottomNavItem("收藏", Icons.Default.Favorite, Screen.Favorites.route)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val downloadsViewModel: DownloadsViewModel = viewModel()

    // 触发下载并跳转到下载页
    val startDownloadAndNavigate: (DownloadParams) -> Unit = { params ->
        downloadsViewModel.startDownload(
            context = context,
            appId = params.appId,
            appName = params.appName,
            packageName = params.packageName,
            vid = params.vid,
            versionName = params.versionName,
            iconUrl = params.iconUrl,
            apkSize = params.apkSize
        )
        // 跳转到下载 tab
        navController.navigate(Screen.Downloads.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Search.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Search.route) {
                SearchScreen(
                    onAppClick = { appId ->
                        navController.navigate(Screen.Detail.createRoute(appId))
                    }
                )
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(viewModel = downloadsViewModel)
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onAppClick = { appId ->
                        navController.navigate(Screen.Detail.createRoute(appId))
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("appId") { type = NavType.StringType })
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getString("appId") ?: return@composable
                DetailScreen(
                    appId = appId,
                    onBackClick = {
                        // pop 回搜索页，保留搜索历史
                        navController.popBackStack(Screen.Search.route, false)
                    },
                    onVersionsClick = { id ->
                        navController.navigate(Screen.Versions.createRoute(id))
                    },
                    onDownload = { params ->
                        startDownloadAndNavigate(params)
                    }
                )
            }

            composable(
                route = Screen.Versions.route,
                arguments = listOf(navArgument("appId") { type = NavType.StringType })
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getString("appId") ?: return@composable
                VersionsScreen(
                    appId = appId,
                    appName = "",
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onVersionClick = { id, vcode ->
                        navController.navigate(Screen.VersionDetail.createRoute(id, vcode))
                    }
                )
            }

            composable(
                route = Screen.VersionDetail.route,
                arguments = listOf(
                    navArgument("appId") { type = NavType.StringType },
                    navArgument("vcode") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val appId = backStackEntry.arguments?.getString("appId") ?: return@composable
                val vcode = backStackEntry.arguments?.getString("vcode") ?: return@composable
                VersionDetailScreen(
                    appId = appId,
                    vcode = vcode,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onDownload = { params ->
                        startDownloadAndNavigate(params)
                    }
                )
            }
        }
    }
}
