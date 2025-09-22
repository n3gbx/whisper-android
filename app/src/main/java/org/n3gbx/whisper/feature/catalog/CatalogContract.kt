package org.n3gbx.whisper.feature.catalog

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.BooksSortType

@Immutable
data class CatalogUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val searchQuery: String? = null,
    val isSearchVisible: Boolean = false,
    val selectedSortOption: SortOption? = null,
) {

    val sortOptions = SortOption.entries
}

@Immutable
enum class SortOption(
    val value: BooksSortType,
    val label: String
) {
    TITLE_ASC(BooksSortType.TITLE_ASC, "Title (A-Z)"),
    TITLE_DESC(BooksSortType.TITLE_DESC, "Title (Z-A)"),
    LENGTH_ASC(BooksSortType.LENGTH_ASC, "Length (Shortest)"),
    LENGTH_DESC(BooksSortType.LENGTH_DESC, "Length (Longest)")
}