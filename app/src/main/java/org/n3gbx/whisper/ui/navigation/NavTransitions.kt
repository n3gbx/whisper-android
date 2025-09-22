package org.n3gbx.whisper.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

private const val NAV_ANIM_DURATION = 300

fun playerEnterTransition(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
}

fun playerExitTransition(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { -it / 3 },
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION,
            easing = EaseInOut
        )
    ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
}

fun playerPopEnterTransition(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { -it / 3 },
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
}

fun playerPopExitTransition(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION,
            easing = EaseInOut
        )
    ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
}

fun tabEnterTransition(): EnterTransition {
    return fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
}

fun tabExitTransition(): ExitTransition {
    return fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
}