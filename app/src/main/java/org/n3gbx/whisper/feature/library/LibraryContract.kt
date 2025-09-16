package org.n3gbx.whisper.feature.library

import androidx.compose.runtime.Immutable
import org.n3gbx.whisper.model.Book

@Immutable
data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val libraryTabs: List<LibraryTab> = LibraryTab.entries,
    val selectedLibraryTabIndex: Int = 0,
) {
    val selectedLibraryTab = libraryTabs[selectedLibraryTabIndex]
}

enum class LibraryTab {
    STARTED,
    FINISHED,
    SAVED;
}
