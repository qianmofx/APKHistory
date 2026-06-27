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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.apkhistory.downloader.data.model.DownloadRecord
import com.apkhistory.downloader.data.model.DownloadStatus
import com.apkhistory.downloader.ui.components.EmptyView
import com.apkhistory.downloader.ui.components.LoadingIndicator
import com.apkhistory.downloader.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                // 应用图标
                AsyncImage(
                    model = record.iconUrl,
                    contentDescription = record.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // 版本信息行：版本号 + 大小 + 状态
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildVersionLabel(record),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(record.status)
                    }
                }
            }

            // 下载中进度条
            if (record.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { record.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
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

            // 完成时间
            if (record.status == DownloadStatus.COMPLETED && record.completedTime > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "下载完成 ${formatTime(record.completedTime)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextTertiary
                )
            }

            // 操作按钮（删除按钮始终显示，其他按钮仅在对应状态下显示）
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
                        Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
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

/** 构建版本标签：版本号 + 大小 */
private fun buildVersionLabel(record: DownloadRecord): String {
    // 豌豆荚页面返回的版本名可能已带"版本号："前缀，清理后统一拼接
    val cleanName = record.versionName
        .replaceFirst(Regex("^版本(号)?[：:]?"), "")
        .trim()
    val base = if (cleanName.isNotEmpty()) "版本 $cleanName" else record.versionName
    return if (record.apkSize.isNotEmpty()) "$base · ${record.apkSize}" else base
}

/** 状态标签 */
@Composable
private fun StatusBadge(status: Int) {
    val (label, color) = when (status) {
        DownloadStatus.COMPLETED -> "已完成" to AppColors.Success
        DownloadStatus.FAILED -> "失败" to AppColors.Error
        DownloadStatus.DOWNLOADING -> "下载中" to AppColors.Primary
        DownloadStatus.PAUSED -> "已暂停" to AppColors.Warning
        else -> "等待中" to AppColors.TextTertiary
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

/** 格式化时间戳为可读时间 */
private fun formatTime(timestamp: Long): String {
    return try {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (_: Exception) { "" }
}
