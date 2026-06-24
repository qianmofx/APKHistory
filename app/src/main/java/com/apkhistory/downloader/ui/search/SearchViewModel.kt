package com.apkhistory.downloader.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apkhistory.downloader.App
import com.apkhistory.downloader.data.model.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val isSearching: Boolean = false,
    val recentSearches: List<String> = emptyList()
)

class SearchViewModel : ViewModel() {
    private val repository = App.getInstance().repository

    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    fun onQueryChanged(query: String) {
        _state.value = _state.value.copy(query = query, isSearching = query.isNotEmpty())
        if (query.length >= 2) {
            search()
        } else if (query.isEmpty()) {
            _state.value = _state.value.copy(
                results = emptyList(),
                error = null,
                isSearching = false
            )
        }
    }

    fun search(page: Int = 1) {
        val query = _state.value.query
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = page == 1,
                error = null,
                currentPage = page
            )
            try {
                val results = repository.searchApps(query, page)
                _state.value = _state.value.copy(
                    results = if (page == 1) results else _state.value.results + results,
                    isLoading = false,
                    hasMore = results.size >= 15,
                    isSearching = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "搜索失败: ${e.message}",
                    isSearching = false
                )
            }
        }
    }

    fun loadMore() {
        if (!_state.value.isLoading && _state.value.hasMore) {
            search(_state.value.currentPage + 1)
        }
    }

    fun clearSearch() {
        _state.value = SearchUiState()
    }
}
