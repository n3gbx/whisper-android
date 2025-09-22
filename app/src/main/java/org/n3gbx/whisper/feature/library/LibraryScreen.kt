package org.n3gbx.whisper.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.model.BooksType
import org.n3gbx.whisper.ui.common.components.BookListItem
import org.n3gbx.whisper.ui.utils.toolbarColors

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    navigateToPlayer: (bookId: Identifier) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryContent(
        uiState = uiState,
        onLibraryTabClick = viewModel::onLibraryTabClick,
        onBookmarkButtonClick = viewModel::onBookmarkButtonClick,
        onBookClick = navigateToPlayer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(
    modifier: Modifier = Modifier,
    uiState: LibraryUiState,
    onLibraryTabClick: (Int) -> Unit,
    onBookmarkButtonClick: (Identifier) -> Unit,
    onBookClick: (Identifier) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Library")
                },
                colors = toolbarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            ScrollableTabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = uiState.selectedBooksTypeIndex,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                uiState.booksTypes.forEachIndexed { index, tab ->
                    val tabTitle = remember(tab) {
                        when (tab) {
                            BooksType.STARTED -> "Started"
                            BooksType.FINISHED -> "Finished"
                            BooksType.SAVED -> "Saved"
                        }
                    }
                    Tab(
                        selected = uiState.selectedBooksTypeIndex == index,
                        onClick = { onLibraryTabClick(index) },
                        text = {
                            Text(text = tabTitle)
                        }
                    )
                }
            }
            Box(
                modifier = Modifier.padding(16.dp)
            ) {
                BooksList(
                    books = uiState.books,
                    showProgress = uiState.selectedBooksType == BooksType.STARTED,
                    onBookClick = onBookClick,
                    onBookmarkButtonClick = onBookmarkButtonClick
                )
            }
        }
    }
}

@Composable
private fun BooksList(
    modifier: Modifier = Modifier,
    books: List<Book>,
    showProgress: Boolean,
    onBookClick: (Identifier) -> Unit,
    onBookmarkButtonClick: (Identifier) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = books,
            key = { it.id.localId }
        ) { book ->
            BookListItem(
                modifier = Modifier.animateItem(),
                book = book,
                showProgress = showProgress,
                onBookmarkButtonClick = { onBookmarkButtonClick(book.id) },
                onClick = { onBookClick(book.id) },
            )
        }
    }
}
