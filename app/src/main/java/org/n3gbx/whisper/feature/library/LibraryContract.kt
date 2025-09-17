package org.n3gbx.whisper.feature.library

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.BooksType

@Immutable
data class LibraryUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val booksTypes: List<BooksType> = BooksType.entries,
    val selectedBooksTypeIndex: Int = 0,
) {
    val selectedBooksType = booksTypes[selectedBooksTypeIndex]
}
