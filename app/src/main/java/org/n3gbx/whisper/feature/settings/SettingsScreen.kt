package org.n3gbx.whisper.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.n3gbx.whisper.ui.utils.bottomNavBarPadding
import org.n3gbx.whisper.ui.utils.toolbarColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState.value,
        onSettingClick = viewModel::onSettingClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    uiState: SettingsUiState,
    onSettingClick: (Setting) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Settings")
                },
                colors = toolbarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .bottomNavBarPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.settings.forEach { (section, settings) ->
                SettingsSection(
                    modifier = modifier,
                    title = section.title
                ) {
                    settings.forEach { setting ->
                        Setting(
                            setting = setting,
                            onSettingClick = onSettingClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Setting(
    modifier: Modifier = Modifier,
    setting: Setting,
    onSettingClick: (Setting) -> Unit
) {
    when (setting) {
        is Setting.Toggle -> {
            ToggleSetting(
                modifier = modifier,
                value = setting.value,
                title = setting.type.title,
                description = setting.type.description,
                onClick = { onSettingClick(setting) }
            )
        }
        is Setting.Value -> {
            SelectSetting(
                modifier = modifier,
                value = setting.value,
                title = setting.type.title,
                description = setting.type.description,
                onClick = { onSettingClick(setting) }
            )
        }
        is Setting.Button -> {
            ButtonSetting(
                modifier = modifier,
                title = setting.type.title,
                onClick = { onSettingClick(setting) }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToggleSetting(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    value: Boolean,
    title: String,
    description: String?,
    onClick: () -> Unit
) {
    Setting(
        modifier = modifier,
        isEnabled = isEnabled,
        onClick = onClick,
        title = {
            Text(text = title)
        },
        description = description?.let {
            {
                Text(
                    text = description,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        value = {
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                Switch(
                    enabled = isEnabled,
                    checked = value,
                    onCheckedChange = { onClick() }
                )
            }
        }
    )
}

@Composable
private fun SelectSetting(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    title: String,
    description: String?,
    value: String,
    onClick: () -> Unit
) {
    Setting(
        modifier = modifier,
        isEnabled = isEnabled,
        onClick = onClick,
        title = {
            Text(text = title)
        },
        description = description?.let {
            {
                Text(
                    text = description,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        value = {
            Text(text = value)
        }
    )
}

@Composable
private fun ButtonSetting(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    title: String,
    onClick: () -> Unit
) {
    Setting(
        modifier = modifier,
        isEnabled = isEnabled,
        onClick = onClick,
        title = {
            Text(
                text = title,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        value = {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    )
}

@Composable
private fun Setting(
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    value: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val primaryContentColor = when {
        isEnabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val secondaryContentColor = when {
        isEnabled -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides primaryContentColor,
                LocalTextStyle provides MaterialTheme.typography.bodyLarge
            ) {
                title()
            }
            description?.let {
                CompositionLocalProvider(
                    LocalContentColor provides secondaryContentColor,
                    LocalTextStyle provides MaterialTheme.typography.bodySmall
                ) {
                    description()
                }
            }
        }
        value?.let {
            CompositionLocalProvider(
                LocalContentColor provides secondaryContentColor,
                LocalTextStyle provides MaterialTheme.typography.bodyMedium
            ) {
                value()
            }
        }
    }
}