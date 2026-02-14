package org.n3gbx.whisper.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BookmarkIcon(
    modifier: Modifier = Modifier,
    isBookmarked: Boolean,
) {
    Icon(
        modifier = modifier,
        imageVector = when {
            isBookmarked -> Icons.Rounded.Bookmark
            else -> Icons.Rounded.BookmarkBorder
        },
        contentDescription = null
    )
}