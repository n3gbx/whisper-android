package org.n3gbx.whisper.feature.miniplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.n3gbx.whisper.feature.player.PlayerViewModel
import org.n3gbx.whisper.ui.common.components.VerticalSlideTransitionWrapper

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    shouldHide: Boolean,
    additionalBottomOffsetDp: Dp = 80.dp,
    onClick: () -> Unit,
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()

    val density = LocalDensity.current
    val bottomOffsetDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + additionalBottomOffsetDp
    val bottomOffsetPx = with(density) { bottomOffsetDp.roundToPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(bottom = bottomOffsetDp)
    ) {
        VerticalSlideTransitionWrapper(
            isVisible = uiState.book != null && !shouldHide,
            initialOffsetY = { fullHeight -> fullHeight + bottomOffsetPx },
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(
                        vertical = 16.dp,
                        horizontal = 12.dp
                    ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .height(64.dp)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Cover(
                            url = uiState.book?.coverUrl
                        )
                        Heading(
                            title = uiState.book?.title.toString(),
                            subtitle = uiState.book?.recentEpisode?.title.toString(),
                        )
                        Controls(
                            isPlaying = uiState.isPlaying,
                            isLoading = uiState.isBuffering,
                            onPlayPauseClick = playerViewModel::onPlayPauseButtonClick,
                            onDismiss = playerViewModel::onMiniPlayerDismiss
                        )
                    }
                    ProgressIndicator(
                        progressValue = uiState.progressValue
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressIndicator(
    modifier: Modifier = Modifier,
    progressValue: Float,
) {
    LinearProgressIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp),
        progress = { progressValue }
    )
}

@Composable
private fun Cover(
    modifier: Modifier = Modifier,
    url: String? = null
) {
    Box(
        modifier = modifier.aspectRatio(1f)
    ) {
        AsyncImage(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
            model = url,
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    }
}

@Composable
private fun RowScope.Heading(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = modifier
            .weight(1f)
            .padding(horizontal = 12.dp)
    ) {
        Text(
            modifier = Modifier.basicMarquee(),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier.basicMarquee(),
            text = "Episode: $subtitle",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.Controls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPauseClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentEnforcement provides false,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayPauseClick
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }

            }
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss"
                )
            }
        }
    }
}