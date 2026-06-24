package com.apkhistory.downloader.ui.downloads

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apkhistory.downloader.data.model.DownloadRecord
import com.apkhistory.downloader.data.model.DownloadStatus
import com.apkhistory.downloader.ui.components.EmptyView
import com.apkhistory.downloader.ui.components.LoadingIndicator
import com.apkhistory.downloader.ui.theme.AppColors

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    when {
        state.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
        state.downloads.isEmpty() -> {
            EmptyView(
                icon = {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AppColors.TextTertiary
                    )
                },
                message = "暂无下载记录"
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.downloads, key = { it.id }) { record ->
                    DownloadItem(
                        record = record,
                        onDelete = { viewModel.deleteDownload(record) },
                        onRetry = { viewModel.retryDownload(context, record) },
                        onInstall = {
                            if (record.status == DownloadStatus.COMPLETED && record.filePath.isNotEmpty()) {
                                viewModel.installApk(context, record.filePath, record.appName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    record: DownloadRecord,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onInstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "版本 ${record.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
                // 状态图标
                Icon(
                    imageVector = when (record.status) {
                        DownloadStatus.COMPLETED -> Icons.Default.CheckCircle
                        DownloadStatus.FAILED -> Icons.Default.Error
                        DownloadStatus.DOWNLOADING -> Icons.Default.Download
                        DownloadStatus.INSTALLING -> Icons.Default.InstallMobile
                        else -> Icons.Default.HourglassEmpty
                    },
                    contentDescription = null,
                    tint = when (record.status) {
                        DownloadStatus.COMPLETED -> AppColors.Success
                        DownloadStatus.FAILED -> AppColors.Error
                        DownloadStatus.DOWNLOADING -> AppColors.Primary
                        else -> AppColors.TextSecondary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            // 进度条
            if (record.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { record.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .let { it },
                    color = AppColors.Primary,
                    trackColor = AppColors.Divider
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${record.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
            }

            // 操作按钮
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.status == DownloadStatus.FAILED) {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重试", style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (record.status == DownloadStatus.COMPLETED) {
                    TextButton(onClick = onInstall) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("安装", style = MaterialTheme.typography.labelLarge)
                    }
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = AppColors.Error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除", style = MaterialTheme.typography.labelLarge, color = AppColors.Error)
                }
            }
        }
    }
}
