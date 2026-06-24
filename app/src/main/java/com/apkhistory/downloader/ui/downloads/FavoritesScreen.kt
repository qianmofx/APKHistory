package com.apkhistory.downloader.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.apkhistory.downloader.data.model.FavoriteApp
import com.apkhistory.downloader.ui.components.EmptyView
import com.apkhistory.downloader.ui.components.LoadingIndicator
import com.apkhistory.downloader.ui.theme.AppColors

@Composable
fun FavoritesScreen(
    onAppClick: (String) -> Unit,
    viewModel: FavoritesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    when {
        state.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
        state.favorites.isEmpty() -> {
            EmptyView(
                icon = {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AppColors.TextTertiary
                    )
                },
                message = "暂无收藏"
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.favorites, key = { it.appId }) { fav ->
                    FavoriteItem(
                        favorite = fav,
                        onClick = { onAppClick(fav.appId) },
                        onRemove = { viewModel.removeFavorite(fav.appId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteItem(
    favorite: FavoriteApp,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = favorite.iconUrl,
                contentDescription = favorite.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favorite.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "版本 ${favorite.currentVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "取消收藏",
                    tint = AppColors.TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
