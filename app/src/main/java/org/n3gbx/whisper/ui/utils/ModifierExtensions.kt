package org.n3gbx.whisper.ui.utils

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.n3gbx.whisper.core.Constants.BOTTOM_NAV_BAR_MIN_HEIGHT

fun Modifier.clearFocusOnTapOutside(focusManager: FocusManager): Modifier = pointerInput(Unit) {
    detectTapGestures(onTap = {
        focusManager.clearFocus()
    })
}

@Composable
fun Modifier.bottomNavBarPadding(): Modifier =
    this then Modifier.padding(bottom = BOTTOM_NAV_BAR_MIN_HEIGHT.dp)