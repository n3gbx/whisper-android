package org.n3gbx.whisper.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.n3gbx.whisper.R
import org.n3gbx.whisper.core.common.GetString
import org.n3gbx.whisper.data.BookRepository
import org.n3gbx.whisper.model.BooksType
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.Result
import org.n3gbx.whisper.model.StringResource
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val getString: GetString,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<LibraryUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val selectedBooksType = MutableStateFlow(_uiState.value.selectedBooksType)

    init {
        observeBooks()
    }

    fun onLibraryTabClick(index: Int) {
        _uiState.update {
            it.copy(
                selectedBooksTypeIndex = index,
                books = emptyList(),
            )
        }
        changeBooksTypeByLibraryTabIndex(index)
    }

    fun onBookmarkButtonClick(bookId: Identifier) {
        viewModelScope.launch {
            bookRepository.updateBookBookmark(bookId)
        }
    }

    private fun observeBooks() {
        selectedBooksType
            .flatMapLatest { bookRepository.getBooks(booksType = it) }
            .onEach { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                books = result.data,
                                isLoading = false,
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvents.emit(LibraryUiEvent.ShowMessage(
                            getString(StringResource.fromRes(R.string.error_generic)))
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun changeBooksTypeByLibraryTabIndex(index: Int) {
        selectedBooksType.value = BooksType.entries[index]
    }
}