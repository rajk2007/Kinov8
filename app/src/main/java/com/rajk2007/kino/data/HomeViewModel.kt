package com.rajk2007.kino.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajk2007.kino.core.HomeSection
import com.rajk2007.kino.core.LoadResponse
import com.rajk2007.kino.core.SearchResponse
import com.rajk2007.kino.providers.MovieBoxProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val sections: List<HomeSection> = emptyList(),
    val searchResults: List<SearchResponse> = emptyList(),
    val searching: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    val movieBoxApi = MovieBoxProvider()
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { movieBoxApi.getMainPage() }
                .onSuccess { _state.value = HomeUiState(loading = false, sections = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "MovieBox is unavailable") }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList(), searching = false)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(searching = true, error = null)
            runCatching { movieBoxApi.search(query) }
                .onSuccess { _state.value = _state.value.copy(searchResults = it, searching = false) }
                .onFailure { _state.value = _state.value.copy(searching = false, error = it.message ?: "Search failed") }
        }
    }
}

data class DetailsUiState(
    val loading: Boolean = true,
    val response: LoadResponse? = null,
    val links: List<com.rajk2007.kino.core.ExtractorLink> = emptyList(),
    val linksLoading: Boolean = false,
    val error: String? = null
)

class DetailsViewModel : ViewModel() {
    val movieBoxApi = MovieBoxProvider()
    private val _state = MutableStateFlow(DetailsUiState())
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    fun load(url: String) {
        viewModelScope.launch {
            _state.value = DetailsUiState(loading = true)
            runCatching { movieBoxApi.load(url) }
                .onSuccess { _state.value = DetailsUiState(loading = false, response = it) }
                .onFailure { _state.value = DetailsUiState(loading = false, error = it.message ?: "Could not load details") }
        }
    }

    fun loadLinks(data: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(linksLoading = true, error = null)
            runCatching { movieBoxApi.loadLinks(data) }
                .onSuccess { _state.value = _state.value.copy(links = it, linksLoading = false) }
                .onFailure { _state.value = _state.value.copy(linksLoading = false, error = it.message ?: "No playable links found") }
        }
    }
}
