package com.apkhistory.downloader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkhistory.downloader.App
import com.apkhistory.downloader.data.model.AppDetail
import com.apkhistory.downloader.data.model.FavoriteApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val detail: AppDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isFavorite: Boolean = false
)

class DetailViewModel : ViewModel() {
    private val repository = App.getInstance().repository

    private val _state = MutableStateFlow(DetailUiState())
    val state = _state.asStateFlow()

    private var currentAppId: String = ""

    fun loadApp(appId: String) {
        if (appId == currentAppId && _state.value.detail != null) return
        currentAppId = appId

        viewModelScope.launch {
            _state.value = DetailUiState(isLoading = true)
            try {
                val detail = repository.getAppDetail(appId)
                val isFav = repository.isFavorite(appId)
                _state.value = DetailUiState(
                    detail = detail,
                    isLoading = false,
                    isFavorite = isFav
                )
            } catch (e: Exception) {
                _state.value = DetailUiState(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }

    fun toggleFavorite() {
        val detail = _state.value.detail ?: return
        viewModelScope.launch {
            if (_state.value.isFavorite) {
                repository.removeFavorite(detail.appId)
                _state.value = _state.value.copy(isFavorite = false)
            } else {
                repository.addFavorite(
                    FavoriteApp(
                        appId = detail.appId,
                        name = detail.name,
                        packageName = detail.packageName,
                        iconUrl = detail.iconUrl,
                        currentVersion = detail.currentVersionName
                    )
                )
                _state.value = _state.value.copy(isFavorite = true)
            }
        }
    }
}
