package org.n3gbx.whisper.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.model.BooksType
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.Result
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    private val booksTypeEvents = MutableSharedFlow<BooksType>()

    init {
        observeBooks()
    }

    fun onLibraryTabClick(index: Int) {
        _uiState.update {
            it.copy(
                selectedBooksTypeIndex = index,
                books = emptyList()
            )
        }
        viewModelScope.launch {
            booksTypeEvents.emit(_uiState.value.selectedBooksType)
        }
    }

    fun onBookmarkButtonClick(bookId: Identifier) {
        viewModelScope.launch {
            bookRepository.updateBookBookmark(bookId)
        }
    }

    private fun observeBooks() {
        booksTypeEvents
            .onStart { emit(_uiState.value.selectedBooksType) }
            .flatMapLatest {
                bookRepository.getBooks(booksType = it)
            }.onEach { result ->
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
            }.launchIn(viewModelScope)
    }
}