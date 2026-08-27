package com.rajk2007.kino.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajk2007.kino.core.SearchResponse
import com.rajk2007.kino.providers.MovieBoxProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResponse> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false
)

class SearchViewModel : ViewModel() {
    private val provider = MovieBoxProvider()
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun onQueryChanged(value: String) {
        _state.value = _state.value.copy(query = value, error = null)
        searchJob?.cancel()
        if (value.trim().isEmpty()) {
            _state.value = _state.value.copy(results = emptyList(), loading = false, hasSearched = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(280)
            _state.value = _state.value.copy(loading = true, hasSearched = true)
            runCatching { provider.search(value.trim()) }
                .onSuccess { _state.value = _state.value.copy(results = it, loading = false) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Search failed") }
        }
    }

    fun retry() { onQueryChanged(_state.value.query) }
}
