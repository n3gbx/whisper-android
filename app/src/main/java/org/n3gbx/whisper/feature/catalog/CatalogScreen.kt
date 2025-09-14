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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import org.n3gbx.whisper.model.Book

@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    navigateToPlayer: (bookId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.isSearchVisible) {
        if (uiState.isSearchVisible) {
            focusRequester.requestFocus()
        }
    }

    CatalogContent(
        uiState = uiState,
        focusRequester = focusRequester,
        onSearchQueryChange = viewModel::onSearchQuery,
        onSearchToggle = viewModel::onSearchToggle,
        onSearchQueryClear = viewModel::onSearchQueryClear,
        onClick = navigateToPlayer,
    )
}

@Composable
private fun CatalogContent(
    modifier: Modifier = Modifier,
    uiState: CatalogUiState,
    focusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryClear: () -> Unit,
    onClick: (bookId: String) -> Unit
) {
    var isGrid by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Toolbar(
                isSearchVisible = uiState.isSearchVisible,
                searchQuery = uiState.searchQuery.orEmpty(),
                onSearchQueryChange = onSearchQueryChange,
                onSearchToggle = onSearchToggle,
                onSearchQueryClear = onSearchQueryClear,
                focusRequester = focusRequester
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable {  },
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
                    modifier = Modifier.clickable {
                        isGrid = !isGrid
                    },
                    imageVector = if (isGrid) Icons.Default.List else Icons.Default.GridView,
                    contentDescription = "Layout",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (isGrid) {
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.books) { book ->
                        BookGridItem(
                            book = book,
                            onBookmarkButtonClick = {},
                            onClick = { onClick(book.id) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.books) { book ->
                        BookListItem(
                            book = book,
                            onBookmarkButtonClick = {},
                            onClick = { onClick(book.id) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    modifier: Modifier = Modifier,
    isSearchVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryClear: () -> Unit,
    focusRequester: FocusRequester
) {
    val focusManager = LocalFocusManager.current

    TopAppBar(
        modifier = modifier,
        title = {
            if (isSearchVisible) {
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .padding(end = 8.dp),
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Title, author or description",
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            } else {
                Text(text = "Catalog")
            }
        },
        actions = {
            if (isSearchVisible) {
                IconButton(
                    onClick = {
                        if (searchQuery.isNotEmpty()) {
                            onSearchQueryClear()
                        } else {
                            onSearchToggle()
                            focusManager.clearFocus()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = if (searchQuery.isNotEmpty()) "Clear search" else "Close search"
                    )
                }
            } else {
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
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
            .clickable { onClick() },
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
                Text(
                    text = "${book.episodes.size} episodes",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

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
            Icon(
                modifier = Modifier.clickable {
                    onBookmarkButtonClick()
                },
                imageVector = Icons.Rounded.BookmarkBorder,
                contentDescription = "Bookmark"
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
            .clickable { onClick() }
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
                Text(
                    text = "${book.episodes.size} episodes",
                    color = Color.White,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    fontSize = 12.sp
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
            Icon(
                modifier = Modifier.clickable {
                    onBookmarkButtonClick()
                },
                imageVector = Icons.Rounded.BookmarkBorder,
                contentDescription = "Bookmark"
            )
        }
    }
}