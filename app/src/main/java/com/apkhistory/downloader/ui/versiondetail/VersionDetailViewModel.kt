package com.apkhistory.downloader.ui.versiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkhistory.downloader.App
import com.apkhistory.downloader.data.model.VersionDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VersionDetailUiState(
    val detail: VersionDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class VersionDetailViewModel : ViewModel() {
    private val repository = App.getInstance().repository

    private val _state = MutableStateFlow(VersionDetailUiState())
    val state = _state.asStateFlow()

    private var currentKey: String = ""

    fun loadVersionDetail(appId: String, vcode: String) {
        val key = "$appId/$vcode"
        if (key == currentKey && _state.value.detail != null) return
        currentKey = key

        viewModelScope.launch {
            _state.value = VersionDetailUiState(isLoading = true)
            try {
                val detail = repository.getVersionDetail(appId, vcode)
                _state.value = VersionDetailUiState(detail = detail, isLoading = false)
            } catch (e: Exception) {
                _state.value = VersionDetailUiState(
                    isLoading = false,
                    error = "加载版本详情失败: ${e.message}"
                )
            }
        }
    }
}
