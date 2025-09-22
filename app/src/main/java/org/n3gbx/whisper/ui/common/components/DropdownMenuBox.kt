package org.n3gbx.whisper.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

@Composable
fun <T> DropdownMenuBox(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    selectedOption: T?,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    onReset: () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        content()

        CompositionLocalProvider(LocalTonalElevationEnabled provides false) {
            DropdownMenu(
                expanded = isVisible,
                onDismissRequest = onDismiss,
            ) {
                options.forEach { option ->
                    val textColor =
                        if (option == selectedOption) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel(option),
                                color = textColor
                            )
                        },
                        onClick = {
                            onDismiss()
                            if (option == selectedOption) {
                                onReset()
                            } else {
                                onSelect(option)
                            }
                        }
                    )
                }
            }
        }
    }
}