package org.n3gbx.whisper.utils

import androidx.compose.ui.Modifier

fun Modifier.applyIf(
    condition: Boolean,
    block: Modifier.() -> Modifier,
) = if (condition) block(this) else this