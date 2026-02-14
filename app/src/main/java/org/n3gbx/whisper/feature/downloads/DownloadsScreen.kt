package org.n3gbx.whisper.feature.downloads

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.n3gbx.whisper.R
import org.n3gbx.whisper.model.Episode
import org.n3gbx.whisper.ui.common.DeleteDialog
import org.n3gbx.whisper.ui.common.EmptyListPlaceholder
import org.n3gbx.whisper.ui.utils.toolbarColors

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (uiState.deleteDialog) {
        is DownloadsUiState.Dialog.DeleteDownloadDialog -> {
            val episode = (uiState.deleteDialog as DownloadsUiState.Dialog.DeleteDownloadDialog).episode
            DeleteDialog(
                title = stringResource(R.string.dialog_delete_title),
                text = stringResource(R.string.dialog_delete_download_text),
                onDismiss = viewModel::onDialogDismiss,
                onConfirm = { viewModel.onDownloadDeleteConfirm(episode) }
            )
        }
        is DownloadsUiState.Dialog.DeleteAllDialog -> {
            DeleteDialog(
                title = stringResource(R.string.dialog_delete_title),
                text = stringResource(R.string.dialog_delete_all_downloads_text),
                onDismiss = viewModel::onDialogDismiss,
                onConfirm = viewModel::onDeleteAllConfirm
            )
        }
        else -> {}
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                DownloadsUiEvent.NavigateBack -> {
                    navigateBack()
                }
                is DownloadsUiEvent.ShowMessage -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DownloadsContent(
        uiState = uiState,
        onDownloadDeleteClick = viewModel::onDownloadDeleteClick,
        onDeleteAllClick = viewModel::onDeleteAllClick,
        onBackButtonClick = viewModel::onBackButtonClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadsContent(
    modifier: Modifier = Modifier,
    uiState: DownloadsUiState,
    onDownloadDeleteClick: (Episode) -> Unit,
    onDeleteAllClick: () -> Unit,
    onBackButtonClick: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Toolbar(
                title = stringResource(R.string.downloads_heading),
                isClearAllButtonVisible = uiState.isClearAllButtonVisible,
                onClearAllButtonClick = onDeleteAllClick,
                onBackButtonClick = onBackButtonClick,
            )
        },
    ) { padding ->
        if (uiState.episodes.isEmpty()) {
            EmptyListPlaceholder(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(start = 24.dp, end = 16.dp)
            ) {
                items(
                    items = uiState.episodes,
                    key = { it.first.id.localId },
                ) { episode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = episode.first.title,
                                style = MaterialTheme.typography.titleSmall,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = episode.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                            IconButton(
                                onClick = { onDownloadDeleteClick(episode.first) }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    contentDescription = null,
                                )
                            }
                        }
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
    title: String,
    isClearAllButtonVisible: Boolean,
    onClearAllButtonClick: () -> Unit,
    onBackButtonClick: () -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(text = title)
        },
        navigationIcon = {
            IconButton(
                onClick = onBackButtonClick
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            if (isClearAllButtonVisible) {
                TextButton(onClick = onClearAllButtonClick) {
                    Text(text = stringResource(R.string.downloads_clear_label))
                }
            }
        },
        colors = toolbarColors(),
    )
}