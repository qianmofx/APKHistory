package com.apkhistory.downloader.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.apkhistory.downloader.data.model.AppDetail
import com.apkhistory.downloader.ui.components.ErrorIndicator
import com.apkhistory.downloader.ui.components.ImagePreviewDialog
import com.apkhistory.downloader.ui.components.LoadingIndicator
import com.apkhistory.downloader.ui.components.ThinTopBar
import com.apkhistory.downloader.ui.navigation.DownloadParams
import com.apkhistory.downloader.ui.theme.AppColors

@Composable
fun DetailScreen(
    appId: String,
    onVersionsClick: (String) -> Unit,
    onDownload: (DownloadParams) -> Unit,
    onBackClick: () -> Unit = {},
    viewModel: DetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(appId) { viewModel.loadApp(appId) }

    Column(modifier = Modifier.fillMaxSize()) {
        ThinTopBar(
            title = state.detail?.name ?: "",
            onBackClick = onBackClick,
            actions = {
                if (state.detail != null) {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (state.isFavorite) "取消收藏" else "收藏",
                            tint = if (state.isFavorite) AppColors.Error else AppColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
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
            state.detail != null -> {
                DetailContent(
                    detail = state.detail!!,
                    onVersionsClick = { onVersionsClick(appId) },
                    onDownload = {
                        val d = state.detail!!
                        onDownload(
                            DownloadParams(
                                appId = d.appId,
                                appName = d.name,
                                packageName = d.packageName,
                                vid = d.currentVid,
                                versionName = d.currentVersionName,
                                iconUrl = d.iconUrl,
                                apkSize = d.size
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    detail: AppDetail,
    onVersionsClick: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previewIndex = remember { mutableStateOf(-1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 头部：图标 + 名称 + 下载按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = detail.iconUrl,
                contentDescription = detail.name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(detail.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("版本 ${detail.currentVersionName}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(detail.size, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
        }

        // 下载按钮区
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDownload,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("下载最新版")
            }

            OutlinedButton(
                onClick = onVersionsClick,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("历史版本")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppColors.Divider)
        Spacer(modifier = Modifier.height(12.dp))

        // 信息行
        InfoRow("大小", detail.size)
        InfoRow("更新", detail.updateDate)
        InfoRow("系统要求", detail.systemRequirement)
        InfoRow("开发者", detail.developer)
        InfoRow("安装量", detail.installCount)
        if (detail.category.isNotEmpty()) InfoRow("分类", detail.category)

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = AppColors.Divider)
        Spacer(modifier = Modifier.height(12.dp))

        // 更新内容
        if (detail.changelog.isNotEmpty()) {
            Text(
                text = "更新内容",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = detail.changelog,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 截图——点击可全屏预览
        if (detail.screenshotUrls.isNotEmpty()) {
            Text(
                text = "截图",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                detail.screenshotUrls.take(5).forEachIndexed { index, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "截图 ${index + 1}/${detail.screenshotUrls.size}",
                        modifier = Modifier
                            .width(160.dp)
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Divider)
                            .clickable { previewIndex.value = index },
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 全屏截图预览
        if (previewIndex.value >= 0) {
            ImagePreviewDialog(
                images = detail.screenshotUrls,
                initialIndex = previewIndex.value,
                onDismiss = { previewIndex.value = -1 }
            )
        }

        // 应用简介
        if (detail.description.isNotEmpty()) {
            Text(
                text = "简介",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = detail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextTertiary,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value.ifEmpty { "--" },
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
