package com.apkhistory.downloader.ui.versions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apkhistory.downloader.data.model.AppVersion
import com.apkhistory.downloader.ui.components.ErrorIndicator
import com.apkhistory.downloader.ui.components.LoadingIndicator
import com.apkhistory.downloader.ui.components.ThinTopBar
import com.apkhistory.downloader.ui.theme.AppColors

@Composable
fun VersionsScreen(
    appId: String,
    onBackClick: () -> Unit,
    onVersionClick: (String, String, String) -> Unit,
    viewModel: VersionsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(appId) {
        viewModel.loadVersions(appId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ThinTopBar(
            title = "历史版本",
            onBackClick = onBackClick
        )

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
            state.error != null -> {
                ErrorIndicator(message = state.error!!)
            }
            state.versions.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无历史版本", color = AppColors.TextSecondary)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.versions, key = { _, v -> "${v.vid}_${v.versionCode}" }) { index, version ->
                        VersionItem(
                            index = index + 1,
                            version = version,
                            onClick = { onVersionClick(appId, version.versionCode, version.vid) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun VersionItem(
    index: Int,
    version: AppVersion,
    onClick: () -> Unit
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
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 序号
            Text(
                text = "#$index",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextTertiary,
                modifier = Modifier.width(36.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "版本 ${version.versionName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                if (version.apkSize.isNotEmpty()) {
                    Text(
                        text = version.apkSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.TextTertiary
            )
        }
    }
}
