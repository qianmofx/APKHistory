package com.apkhistory.downloader.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Search : Screen("search", "搜索", Icons.Default.Search)
    data object Downloads : Screen("downloads", "下载", Icons.Default.Download)
    data object Favorites : Screen("favorites", "收藏", Icons.Default.Favorite)
    data object Detail : Screen("detail/{appId}", "详情") {
        fun createRoute(appId: String) = "detail/$appId"
    }
    data object Versions : Screen("versions/{appId}", "历史版本") {
        fun createRoute(appId: String) = "versions/$appId"
    }
    data object VersionDetail : Screen("version_detail/{appId}/{vcode}/{vid}", "版本详情") {
        fun createRoute(appId: String, vcode: String, vid: String) = "version_detail/$appId/$vcode/$vid"
    }

    companion object {
        val bottomNavItems = listOf(Search, Downloads, Favorites)
    }
}
