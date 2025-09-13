package org.n3gbx.whisper.ui.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VerticalSlideTransitionWrapper(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    initialOffsetY: (fullHeight: Int) -> Int = { it },
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)) + slideInVertically(
            initialOffsetY = initialOffsetY,
            animationSpec = tween(durationMillis = 300)
        ),
        exit = fadeOut(animationSpec = tween(durationMillis = 200)) + slideOutVertically(
            targetOffsetY = initialOffsetY,
            animationSpec = tween(durationMillis = 300)
        ),
        content = content
    )
}