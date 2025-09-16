package org.n3gbx.whisper.feature.catalog

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.model.Book

@Immutable
data class CatalogUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val searchQuery: String? = null,
    val isSearchVisible: Boolean = false,
)