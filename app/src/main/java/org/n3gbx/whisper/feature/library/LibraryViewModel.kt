package org.n3gbx.whisper.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.data.BookmarkRepository
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        observeBooks()
    }

    fun onLibraryTabClick(index: Int) {
        _uiState.update {
            it.copy(selectedLibraryTabIndex = index)
        }
    }

    fun onBookmarkButtonClick(bookId: String) {
        viewModelScope.launch {
            bookmarkRepository.changeBookmark(bookId)
        }
    }

    private fun observeBooks() {
        viewModelScope.launch {
            bookRepository.getBooks().collect { books ->
                _uiState.update {
                    it.copy(books = books)
                }
            }
        }
    }
}