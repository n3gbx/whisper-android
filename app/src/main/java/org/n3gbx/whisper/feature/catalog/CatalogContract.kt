package org.n3gbx.whisper.feature.catalog

import org.n3gbx.whisper.model.Book

data class CatalogUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val searchQuery: String? = null,
    val isSearchVisible: Boolean = false,
)