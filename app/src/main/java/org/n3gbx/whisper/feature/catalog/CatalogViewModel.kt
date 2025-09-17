package org.n3gbx.whisper.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.Result
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val bookRepository: BookRepository,
): ViewModel() {

    private val getBooksTriggerEvents = MutableSharedFlow<GetBooksTrigger>()

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState

    init {
        observeSearchQuery()
    }

    fun onSearchQuery(query: String) {
        _uiState.update {
            it.copy(searchQuery = query)
        }
        viewModelScope.launch {
            getBooksTriggerEvents.emit(GetBooksTrigger.Search(query))
        }
    }

    fun onSearchQueryClear() {
        _uiState.update {
            it.copy(searchQuery = null)
        }
        viewModelScope.launch {
            getBooksTriggerEvents.emit(GetBooksTrigger.Search(null))
        }
    }

    fun onSearchToggle() {
        _uiState.update {
            it.copy(isSearchVisible = !it.isSearchVisible)
        }
    }

    fun onBookmarkButtonClick(bookId: Identifier) {
        viewModelScope.launch {
            bookRepository.updateBookBookmark(bookId)
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            getBooksTriggerEvents.emit(GetBooksTrigger.Refresh)
        }
    }

    private fun observeSearchQuery() {
        getBooksTriggerEvents
            .onStart { emit(GetBooksTrigger.Search(_uiState.value.searchQuery)) }
            .debounce { if (it is GetBooksTrigger.Search) 300L else 0L}
            .flatMapLatest { event ->
                val shouldRefresh = (event as? GetBooksTrigger.Refresh) != null
                val query = (event as? GetBooksTrigger.Search)?.query

                bookRepository.getBooks(query, shouldRefresh)
            }
            .onEach { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update {
                            it.copy(isLoading = true)
                        }
                    }
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                books = result.data,
                                isLoading = false
                            )
                        }
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    private sealed interface GetBooksTrigger {
        data object Refresh: GetBooksTrigger
        data class Search(val query: String?): GetBooksTrigger
    }
}