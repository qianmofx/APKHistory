package com.apkhistory.downloader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.apkhistory.downloader.data.model.SearchResult
import com.apkhistory.downloader.ui.theme.AppColors

/** 搜索结果卡片 */
@Composable
fun AppCard(
    app: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = app.iconUrl,
                contentDescription = app.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (app.versionName.isNotEmpty()) {
                    Text(
                        text = "版本 ${app.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
                if (app.installCount.isNotEmpty()) {
                    Text(
                        text = app.installCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextTertiary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.TextTertiary
            )
        }
    }
}

/** 加载/错误指示器 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = AppColors.Primary,
            strokeWidth = 3.dp
        )
    }
}

@Composable
fun ErrorIndicator(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

/** 占位视图 */
@Composable
fun EmptyView(
    icon: @Composable () -> Unit,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
    }
}

/** 信息行：用于详情页/版本详情页的字段展示 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
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
