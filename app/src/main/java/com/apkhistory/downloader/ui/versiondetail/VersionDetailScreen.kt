package com.apkhistory.downloader.ui.versiondetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apkhistory.downloader.ui.components.ErrorIndicator
import com.apkhistory.downloader.ui.components.LoadingIndicator
import com.apkhistory.downloader.ui.components.ThinTopBar
import com.apkhistory.downloader.ui.navigation.DownloadParams
import com.apkhistory.downloader.ui.theme.AppColors

@Composable
fun VersionDetailScreen(
    appId: String,
    vcode: String,
    appName: String = "",
    onBackClick: () -> Unit,
    onDownload: (DownloadParams) -> Unit,
    viewModel: VersionDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(appId, vcode) {
        viewModel.loadVersionDetail(appId, vcode)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ThinTopBar(
            title = state.detail?.versionName?.let { "版本 $it" } ?: "版本详情",
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
            state.detail != null -> {
                val detail = state.detail!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 下载按钮
                    Button(
                        onClick = {
                            onDownload(
                                DownloadParams(
                                    appId = appId,
                                    appName = detail.appName.ifEmpty { appName },
                                    packageName = "",
                                    vid = detail.vid,
                                    versionName = detail.versionName,
                                    iconUrl = "",
                                    apkSize = detail.apkSize
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("下载此版本", style = MaterialTheme.typography.titleSmall)
                    }

                    // 信息卡片
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow("版本", detail.versionName, isBold = true)
                            InfoRow("发布日期", detail.updateDate)
                            InfoRow("大小", detail.apkSize)
                            InfoRow("系统要求", detail.systemRequirement)
                            InfoRow("开发者", detail.developer)
                            InfoRow("分类", detail.category)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 更新内容
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "更新内容",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = detail.changelog,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 权限
                    if (detail.permissions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "权限要求",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                detail.permissions.forEach { perm ->
                                    Text(
                                        text = "• $perm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextTertiary,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextPrimary,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
    }
}
