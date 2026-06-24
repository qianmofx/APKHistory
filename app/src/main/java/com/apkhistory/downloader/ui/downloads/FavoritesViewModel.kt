package com.apkhistory.downloader.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkhistory.downloader.App
import com.apkhistory.downloader.data.model.FavoriteApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favorites: List<FavoriteApp> = emptyList(),
    val isLoading: Boolean = true
)

class FavoritesViewModel : ViewModel() {
    private val repository = App.getInstance().repository

    private val _state = MutableStateFlow(FavoritesUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllFavorites().collect { favorites ->
                _state.value = FavoritesUiState(favorites = favorites, isLoading = false)
            }
        }
    }

    fun removeFavorite(appId: String) {
        viewModelScope.launch {
            repository.removeFavorite(appId)
        }
    }
}
