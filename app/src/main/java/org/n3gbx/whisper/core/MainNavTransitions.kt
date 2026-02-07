package org.n3gbx.whisper.core

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val NAV_ANIM_DURATION = 300

fun enterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
}

fun exitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
}

fun popEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
}

fun popExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(
            durationMillis = NAV_ANIM_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
}