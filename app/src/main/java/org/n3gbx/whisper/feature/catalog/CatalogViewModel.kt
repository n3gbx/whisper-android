package org.n3gbx.whisper.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.data.BookmarkRepository
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val bookmarkRepository: BookmarkRepository,
): ViewModel() {

    private val searchQueryState = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState

    init {
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

    fun onBookmarkButtonClick(bookId: String) {
        viewModelScope.launch {
            bookmarkRepository.changeBookmark(bookId)
        }
    }

    private fun observeSearchQuery() {
        combine(
            searchQueryState,
            searchQueryState
                .debounce(300)
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