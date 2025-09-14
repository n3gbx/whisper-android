package org.n3gbx.whisper.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.data.BookRepository
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val bookRepository: BookRepository
): ViewModel() {

    private val searchQueryState = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState

    init {
        observeBooks()
        observeSearchQuery()
    }

    fun onSearchQuery(query: String) {
        searchQueryState.value = query
    }

    fun onSearchQueryClear() {
        searchQueryState.value = null
    }

    fun onSearchToggle() {
        _uiState.update {
            it.copy(isSearchVisible = !it.isSearchVisible)
        }
    }

    private fun observeBooks() {
        viewModelScope.launch {
            bookRepository.getBooks().collect { books ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        books = books
                    )
                }
            }
        }
    }

    private fun observeSearchQuery() {
        combine(
            searchQueryState,
            searchQueryState
                .debounce(200)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    bookRepository.getBooks().map { items ->
                        items.filter { it.matchesQuery(query) }
                    }
                }
        ) { query, books ->
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    books = books,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }
}