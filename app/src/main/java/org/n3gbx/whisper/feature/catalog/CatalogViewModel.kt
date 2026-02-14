package org.n3gbx.whisper.feature.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.data.SettingsRepository
import org.n3gbx.whisper.model.BooksSortType
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.Result
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository,
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

    fun onSortOptionChange(option: SortOption?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(selectedSortOption = option)
            }
            getBooksTriggerEvents.emit(GetBooksTrigger.Sort(option?.value))
        }
    }

    fun onLayoutToggle() {
        viewModelScope.launch {
            settingsRepository.setCatalogGridLayoutSetting(!_uiState.value.isGridLayout)
        }
    }

    private fun observeSearchQuery() {
        getBooksTriggerEvents
            .onStart { emit(GetBooksTrigger.Search(_uiState.value.searchQuery)) }
            .debounce { if (it is GetBooksTrigger.Search) 300L else 0L}
            .flatMapLatest { event ->
                val shouldRefresh = (event as? GetBooksTrigger.Refresh) != null
                val query = (event as? GetBooksTrigger.Search)?.query
                val sortType = (event as? GetBooksTrigger.Sort)?.sortType

                combine(
                    flow = bookRepository.getBooks(
                        query = query,
                        shouldRefresh = shouldRefresh,
                        booksSortType = sortType,
                    ),
                    flow2 = settingsRepository.getCatalogGridLayoutSetting(),
                    transform = ::Pair,
                )
            }
            .onEach { (booksResult, isGridLayout) ->
                when (booksResult) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                books = booksResult.data,
                                isGridLayout = isGridLayout,
                                isLoading = false
                            )
                        }
                    }
                    else -> {
                        _uiState.update {
                            it.copy(isLoading = true)
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private sealed interface GetBooksTrigger {
        data object Refresh: GetBooksTrigger
        data class Sort(val sortType: BooksSortType?): GetBooksTrigger
        data class Search(val query: String?): GetBooksTrigger
    }
}