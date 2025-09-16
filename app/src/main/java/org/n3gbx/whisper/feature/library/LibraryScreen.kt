package org.n3gbx.whisper.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.ui.common.components.BookmarkIcon
import org.n3gbx.whisper.ui.common.components.TotalDuration

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    navigateToPlayer: (bookId: String) -> Unit
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
    onBookmarkButtonClick: (String) -> Unit,
    onBookClick: (String) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Library")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                selectedTabIndex = uiState.selectedLibraryTabIndex
            ) {
                uiState.libraryTabs.forEachIndexed { index, tab ->
                    val tabTitle = remember(tab) {
                        when(tab) {
                            LibraryTab.STARTED -> "Started"
                            LibraryTab.FINISHED -> "Finished"
                            LibraryTab.SAVED -> "Saved"
                        }
                    }
                    Tab(
                        selected = uiState.selectedLibraryTabIndex == index,
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
                when (uiState.selectedLibraryTab) {
                    LibraryTab.STARTED -> {
                        StartedLibraryTab(
                            books = uiState.books,
                            onBookClick = onBookClick,
                            onBookmarkButtonClick = onBookmarkButtonClick,
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun StartedLibraryTab(
    modifier: Modifier = Modifier,
    books: List<Book>,
    onBookClick: (String) -> Unit,
    onBookmarkButtonClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = books,
            key = { it.id }
        ) { book ->
            BookItem(
                book = book,
                onBookmarkButtonClick = { onBookmarkButtonClick(book.id) },
                onClick = { onBookClick(book.id) },
            )
        }
    }
}

@Composable
private fun BookItem(
    modifier: Modifier = Modifier,
    book: Book,
    onBookmarkButtonClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .aspectRatio(1f)
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
                model = book.coverUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                TotalDuration(
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = Color.White,
                    totalDuration = book.totalDuration
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = book.author,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.width(56.dp),
                        progress = { book.progressValue },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "${book.progressPercentage}%",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            BookmarkIcon(
                modifier = Modifier.clickable(
                    onClick = onBookmarkButtonClick
                ),
                isBookmarked = book.isBookmarked
            )
        }
    }
}