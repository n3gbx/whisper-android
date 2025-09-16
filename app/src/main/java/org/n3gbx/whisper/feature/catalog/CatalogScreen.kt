package org.n3gbx.whisper.feature.catalog

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import coil.compose.rememberAsyncImagePainter
import com.valentinilk.shimmer.shimmer
import org.n3gbx.whisper.model.Book
import org.n3gbx.whisper.ui.common.components.BookmarkIcon
import org.n3gbx.whisper.ui.common.components.SearchToolbar
import org.n3gbx.whisper.ui.common.components.TotalDuration

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    navigateToPlayer: (bookId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isGridLayout by rememberSaveable { mutableStateOf(false) }

    val toggleLayout: () -> Unit = { isGridLayout = !isGridLayout }

    CatalogContent(
        uiState = uiState,
        isGridLayout = isGridLayout,
        onSearchQueryChange = viewModel::onSearchQuery,
        onSearchToggle = viewModel::onSearchToggle,
        onSearchQueryClear = viewModel::onSearchQueryClear,
        onBookmarkButtonClick = viewModel::onBookmarkButtonClick,
        onClick = navigateToPlayer,
        onLayoutToggle = toggleLayout,
    )
}

@Composable
private fun CatalogContent(
    modifier: Modifier = Modifier,
    uiState: CatalogUiState,
    isGridLayout: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryClear: () -> Unit,
    onClick: (bookId: String) -> Unit,
    onBookmarkButtonClick: (bookId: String) -> Unit,
    onLayoutToggle: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SearchToolbar(
                isSearchVisible = uiState.isSearchVisible,
                searchQuery = uiState.searchQuery.orEmpty(),
                onSearchQueryChange = onSearchQueryChange,
                onSearchToggle = onSearchToggle,
                onSearchQueryClear = onSearchQueryClear,
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (uiState.isLoading) {
                CatalogLoading()
            } else {
                Catalog(
                    uiState = uiState,
                    isGridLayout = isGridLayout,
                    onBookmarkButtonClick = onBookmarkButtonClick,
                    onClick = onClick,
                    onLayoutToggle = onLayoutToggle
                )
            }
        }
    }
}

@Composable
private fun Catalog(
    uiState: CatalogUiState,
    isGridLayout: Boolean,
    onBookmarkButtonClick: (bookId: String) -> Unit,
    onClick: (bookId: String) -> Unit,
    onLayoutToggle: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        CatalogHeader(
            isGridLayout = isGridLayout,
            onLayoutToggle = onLayoutToggle
        )
        if (isGridLayout) {
            BooksGrid(
                books = uiState.books,
                onBookmarkButtonClick = onBookmarkButtonClick,
                onClick = onClick
            )
        } else {
            BooksList(
                books = uiState.books,
                onBookmarkButtonClick = onBookmarkButtonClick,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun CatalogHeader(
    modifier: Modifier = Modifier,
    isGridLayout: Boolean,
    onLayoutToggle: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable {},
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Sort",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Icon(
            modifier = Modifier.clickable(onClick = onLayoutToggle),
            imageVector = if (isGridLayout) Icons.Default.List else Icons.Default.GridView,
            contentDescription = "Layout",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@NonRestartableComposable
@Composable
private fun BooksGrid(
    modifier: Modifier = Modifier,
    books: List<Book>,
    onBookmarkButtonClick: (String) -> Unit,
    onClick: (String) -> Unit
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = books,
            key = { it.id }
        ) { book ->
            BookGridItem(
                book = book,
                onBookmarkButtonClick = { onBookmarkButtonClick(book.id) },
                onClick = { onClick(book.id) },
            )
        }
    }
}

@NonRestartableComposable
@Composable
private fun BooksList(
    modifier: Modifier = Modifier,
    books: List<Book>,
    onBookmarkButtonClick: (String) -> Unit,
    onClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = books,
            key = { it.id }
        ) { book ->
            BookListItem(
                book = book,
                onBookmarkButtonClick = { onBookmarkButtonClick(book.id) },
                onClick = { onClick(book.id) },
            )
        }
    }
}

@Composable
private fun BookListItem(
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
                .size(80.dp)
                .aspectRatio(1f)
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(6.dp))
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
                            bottomStart = 6.dp,
                            bottomEnd = 6.dp
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
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = book.title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = book.author,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            BookmarkIcon(
                modifier = Modifier.clickable(onClick = onBookmarkButtonClick),
                isBookmarked = book.isBookmarked
            )
        }
    }
}

@Composable
private fun BookGridItem(
    modifier: Modifier = Modifier,
    book: Book,
    onBookmarkButtonClick: () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.aspectRatio(1f)
        ) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                painter = rememberAsyncImagePainter(book.coverUrl),
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
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = book.author,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    style = MaterialTheme.typography.bodySmall
                )
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

@Composable
private fun CatalogLoading(
    modifier: Modifier = Modifier)
{
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .shimmer()
                    .size(width = 48.dp, height = 16.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(
                modifier = Modifier
                    .shimmer()
                    .size(width = 16.dp, height = 16.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        repeat(5) {
            Row(
                modifier = Modifier
                    .shimmer()
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(12.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }
    }
}