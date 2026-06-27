package com.apkhistory.downloader.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apkhistory.downloader.ui.components.AppCard
import com.apkhistory.downloader.ui.components.ErrorIndicator
import com.apkhistory.downloader.ui.components.LoadingIndicator
import com.apkhistory.downloader.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAppClick: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // 滚动加载更多
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && state.hasMore && !state.isLoading) {
            viewModel.loadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 搜索栏
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.onQueryChanged(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("搜索安卓应用…", color = AppColors.TextTertiary) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = AppColors.TextSecondary)
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除", tint = AppColors.TextSecondary)
                    }
                }
            },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = AppColors.Border,
                unfocusedContainerColor = AppColors.Surface
            ),
            singleLine = true
        )

        // 内容
        when {
            state.isLoading && state.results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
            state.error != null && state.results.isEmpty() -> {
                ErrorIndicator(message = state.error!!, onRetry = { viewModel.search() })
            }
            state.results.isNotEmpty() -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.results, key = { it.appId + it.vid }) { app ->
                        AppCard(app = app, onClick = { onAppClick(app.appId) })
                    }
                    if (state.isLoading) {
                        item { LoadingIndicator(modifier = Modifier.padding(16.dp)) }
                    }
                    if (!state.hasMore && state.results.isNotEmpty()) {
                        item {
                            Text(
                                text = "— 没有更多结果 —",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextTertiary
                            )
                        }
                    }
                }
            }
            state.query.isEmpty() -> {
                // 首页推荐（搜索前显示热门）
                EmptySearchHint()
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有找到「${state.query}」的结果", color = AppColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun EmptySearchHint() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = AppColors.TextTertiary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "搜索任意安卓应用",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary
        )
        Text(
            text = "查看历史版本并下载 APK",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}
