package org.n3gbx.whisper.feature.catalog

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.R
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.BooksSortType
import org.n3gbx.whisper.model.StringResource
import org.n3gbx.whisper.model.StringResource.Companion.fromRes

@Immutable
data class CatalogUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val searchQuery: String? = null,
    val isGridLayout: Boolean = false,
    val isSearchVisible: Boolean = false,
    val selectedSortOption: SortOption? = null,
) {

    val sortOptions = SortOption.entries
}

@Immutable
enum class SortOption(
    val value: BooksSortType,
    val label: StringResource
) {
    TITLE_ASC(BooksSortType.TITLE_ASC, fromRes(R.string.catalog_sort_option_title_a_z)),
    TITLE_DESC(BooksSortType.TITLE_DESC, fromRes(R.string.catalog_sort_option_title_z_a)),
    LENGTH_ASC(BooksSortType.LENGTH_ASC, fromRes(R.string.catalog_sort_option_length_shortest)),
    LENGTH_DESC(BooksSortType.LENGTH_DESC, fromRes(R.string.catalog_sort_option_length_longest))
}