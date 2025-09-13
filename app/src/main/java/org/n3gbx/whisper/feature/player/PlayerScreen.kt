package org.n3gbx.whisper.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.n3gbx.whisper.ui.theme.WhisperTheme

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    navigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PlayerContent(
        uiState = uiState,
        onPlayPauseClick = viewModel::onPlayPauseButtonClick,
        onRewindBackwardClick = viewModel::onRewindBackwardButtonClick,
        onRewindForwardClick = viewModel::onRewindForwardButtonClick,
        onSliderValueChange = viewModel::onSliderValueChange,
        onSliderValueChangeFinished = viewModel::onSliderValueChangeFinished,
        onBackButtonClick = navigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerContent(
    modifier: Modifier = Modifier,
    uiState: PlayerUiState,
    onPlayPauseClick: () -> Unit = {},
    onRewindBackwardClick: () -> Unit = {},
    onRewindForwardClick: () -> Unit = {},
    onSliderValueChange: (Float) -> Unit = {},
    onSliderValueChangeFinished: () -> Unit = {},
    onMoreButtonClick: () -> Unit = {},
    onBookmarkButtonClick: () -> Unit = {},
    onBackButtonClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBackButtonClick
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onMoreButtonClick
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notes,
                            contentDescription = "More"
                        )
                    }
                    IconButton(
                        onClick = onBookmarkButtonClick
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkBorder,
                            contentDescription = "Bookmark"
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Cover(
                url = uiState.currentMedia?.mediaMetadata?.artworkUri.toString()
            )
            Heading(
                title = uiState.currentMedia?.mediaMetadata?.title.toString(),
                author = uiState.currentMedia?.mediaMetadata?.artist.toString(),
            )
            Slider(
                currentTime = uiState.formattedCurrentTime,
                remainingTime = uiState.formattedRemainingTime,
                value = uiState.sliderValue,
                valueRange = uiState.sliderValueRange,
                onValueChange = onSliderValueChange,
                onValueChangeFinished = onSliderValueChangeFinished,
            )
            Controls(
                isPlaying = uiState.isPlaying,
                isLoading = uiState.isLoading,
                onPlayPauseClick = onPlayPauseClick,
                onRewindBackwardClick = onRewindBackwardClick,
                onRewindForwardClick = onRewindForwardClick
            )
            Episodes()
        }
    }
}

@Composable
private fun Cover(
    modifier: Modifier = Modifier,
    url: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
            model = url,
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    }
}

@Composable
private fun Episodes(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 32.dp)
    ) {
        Text(
            text = "Episodes",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun Heading(
    modifier: Modifier = Modifier,
    title: String,
    author: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = author,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun Slider(
    modifier: Modifier = Modifier,
    currentTime: String,
    remainingTime: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (newValue: Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            valueRange = valueRange,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentTime,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = remainingTime,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Controls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isLoading: Boolean = false,
    onPlayPauseClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onRewindBackwardClick: () -> Unit = {},
    onRewindForwardClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CompositionLocalProvider(
            LocalMinimumInteractiveComponentEnforcement provides false,
        ) {
            IconButton(
                onClick = onSpeedClick
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Rewind 10 seconds backward"
                )
            }
            IconButton(
                onClick = onRewindBackwardClick
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    imageVector = Icons.Rounded.Replay10,
                    contentDescription = "Rewind 10 seconds backward"
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface)
                    .clickable {
                        onPlayPauseClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.surface,
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.surface,
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
            }
            IconButton(
                onClick = onRewindForwardClick
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    imageVector = Icons.Rounded.Forward10,
                    contentDescription = "Rewind 10 seconds forward"
                )
            }
            IconButton(
                onClick = onSleepTimerClick
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = "Sleep Timer"
                )
            }
        }
    }
}

@Preview
@Composable
private fun PlayerContentPreview() {
    WhisperTheme {
        PlayerContent(
            uiState = PlayerUiState(),
            onBackButtonClick = {}
        )
    }
}