package org.n3gbx.whisper.ui.common.utils

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.clearFocusOnTapOutside(focusManager: FocusManager): Modifier = pointerInput(Unit) {
    detectTapGestures(onTap = {
        focusManager.clearFocus()
    })
}