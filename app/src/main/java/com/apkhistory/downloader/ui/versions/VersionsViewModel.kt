package com.apkhistory.downloader.ui.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkhistory.downloader.App
import com.apkhistory.downloader.data.model.AppVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VersionsUiState(
    val versions: List<AppVersion> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class VersionsViewModel : ViewModel() {
    private val repository = App.getInstance().repository

    private val _state = MutableStateFlow(VersionsUiState())
    val state = _state.asStateFlow()

    private var currentAppId: String = ""

    fun loadVersions(appId: String) {
        if (appId == currentAppId && _state.value.versions.isNotEmpty()) return
        currentAppId = appId

        viewModelScope.launch {
            _state.value = VersionsUiState(isLoading = true)
            try {
                val versions = repository.getVersions(appId)
                _state.value = VersionsUiState(
                    versions = versions,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = VersionsUiState(
                    isLoading = false,
                    error = "加载版本列表失败: ${e.message}"
                )
            }
        }
    }
}
