package org.n3gbx.whisper.feature.player

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.n3gbx.whisper.model.BookEpisode
import org.n3gbx.whisper.model.Identifier
import org.n3gbx.whisper.ui.common.components.BookmarkIcon
import org.n3gbx.whisper.ui.common.utils.convertToTime
import org.n3gbx.whisper.ui.theme.WhisperTheme

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    bookId: Identifier?,
    navigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(bookId) {
        viewModel.setBook(bookId)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                PlayerUiEvent.NavigateBack -> {
                    navigateBack()
                }
                is PlayerUiEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    PlayerContent(
        uiState = uiState,
        onPlayPauseClick = viewModel::onPlayPauseButtonClick,
        onRewindBackwardClick = viewModel::onRewindBackwardButtonClick,
        onRewindForwardClick = viewModel::onRewindForwardButtonClick,
        onSliderValueChange = viewModel::onSliderValueChange,
        onSliderValueChangeFinished = viewModel::onSliderValueChangeFinished,
        onBookmarkButtonClick = viewModel::onBookmarkButtonClick,
        onEpisodeClick = viewModel::onEpisodeClick,
        onBackButtonClick = navigateBack
    )
}

@Composable
private fun PlayerContent(
    modifier: Modifier = Modifier,
    uiState: PlayerUiState,
    onPlayPauseClick: () -> Unit = {},
    onRewindBackwardClick: () -> Unit = {},
    onRewindForwardClick: () -> Unit = {},
    onSliderValueChange: (Float) -> Unit = {},
    onSliderValueChangeFinished: (Float) -> Unit = {},
    onMoreButtonClick: () -> Unit = {},
    onBookmarkButtonClick: () -> Unit = {},
    onEpisodeClick: (Int) -> Unit = {},
    onBackButtonClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Toolbar(
                title = uiState.book?.recentEpisode?.title,
                isBookmarked = uiState.book?.isBookmarked,
                onBackButtonClick = onBackButtonClick,
                onBookmarkButtonClick = onBookmarkButtonClick,
                onMoreButtonClick = onMoreButtonClick
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Cover(
                url = uiState.book?.coverUrl
            )
            if (uiState.isLoading) {
                HeadingLoading()
            } else {
                Heading(
                    title = uiState.book?.title.toString(),
                    author = uiState.book?.author.toString(),
                )
            }
            Slider(
                isEnabled = !uiState.shouldDisableControls,
                currentTime = uiState.currentTime.convertToTime(),
                remainingTime = uiState.remainingTime.convertToTime(),
                value = uiState.sliderValue,
                valueRange = uiState.sliderValueRange,
                onValueChange = onSliderValueChange,
                onValueChangeFinished = onSliderValueChangeFinished,
            )
            Controls(
                isPlaying = uiState.isPlaying,
                isBuffering = uiState.isBuffering,
                isEnabled = !uiState.shouldDisableControls,
                onPlayPauseClick = onPlayPauseClick,
                onRewindBackwardClick = onRewindBackwardClick,
                onRewindForwardClick = onRewindForwardClick
            )
            Episodes(
                recentEpisodeIndex = uiState.book?.recentEpisodeIndex,
                episodes = uiState.book?.episodes,
                onEpisodeClick = onEpisodeClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    modifier: Modifier = Modifier,
    title: String?,
    isBookmarked: Boolean?,
    onBackButtonClick: () -> Unit,
    onBookmarkButtonClick: () -> Unit,
    onMoreButtonClick: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        title = {
            title?.let {
                Text(
                    modifier = Modifier.basicMarquee(),
                    text = "Episode: $title",
                )
            }
        },
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
                BookmarkIcon(
                    isBookmarked = isBookmarked == true
                )
            }
        }
    )
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
            .padding(top = 16.dp)
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
    recentEpisodeIndex: Int?,
    episodes: List<BookEpisode>?,
    onEpisodeClick: (Int) -> Unit
) {
    if (episodes != null && recentEpisodeIndex != null) {
        Column(
            modifier = modifier.padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Episodes",
                style = MaterialTheme.typography.titleMedium
            )
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                episodes.forEachIndexed { index, episode ->
                    key(episode.id.externalId) {
                        val color =
                            if (index == recentEpisodeIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { onEpisodeClick(index) },
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.titleSmall,
                                color = color
                            )
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = episode.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = color,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                )
                                Text(
                                    text = episode.duration.convertToTime(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                            Text(
                                text = "${episode.progressPercentage}%",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadingLoading(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(24.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
private fun Heading(
    modifier: Modifier = Modifier,
    title: String,
    author: String,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
    ) {
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

@Composable
private fun Slider(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    currentTime: String,
    remainingTime: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (newValue: Float) -> Unit,
    onValueChangeFinished: (currentValue: Float) -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            enabled = isEnabled,
            valueRange = valueRange,
            onValueChange = onValueChange,
            onValueChangeFinished = {
                onValueChangeFinished(value)
            },
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
    isBuffering: Boolean = false,
    isEnabled: Boolean = false,
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
                onClick = onSpeedClick,
                enabled = isEnabled
            ) {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = "Rewind 10 seconds backward"
                )
            }
            IconButton(
                onClick = onRewindBackwardClick,
                enabled = isEnabled
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
                    .clickable(enabled = isEnabled) {
                        onPlayPauseClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
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
                onClick = onRewindForwardClick,
                enabled = isEnabled
            ) {
                Icon(
                    modifier = Modifier.size(48.dp),
                    imageVector = Icons.Rounded.Forward10,
                    contentDescription = "Rewind 10 seconds forward"
                )
            }
            IconButton(
                onClick = onSleepTimerClick,
                enabled = isEnabled
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