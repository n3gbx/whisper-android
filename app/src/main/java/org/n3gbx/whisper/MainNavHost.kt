package org.n3gbx.whisper

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import org.n3gbx.whisper.feature.home.HomeScreen
import org.n3gbx.whisper.feature.player.PlayerScreen
import org.n3gbx.whisper.feature.player.PlayerViewModel
import org.n3gbx.whisper.feature.settings.SettingsScreen
import org.n3gbx.whisper.feature.shelf.ShelfScreen

@ExperimentalFoundationApi
@Composable
fun MainNavHost(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    navController: NavHostController,
    onPlayClick: () -> Unit
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = "home_root",
        enterTransition = { fadeIn(animationSpec = tween(0)) },
        exitTransition = { fadeOut(animationSpec = tween(0)) }
    ) {
        homeGraph(navController, onPlayClick)
        shelfGraph(navController)
        settingsGraph(navController)
        playerGraph(navController, playerViewModel)
    }
}

fun NavGraphBuilder.homeGraph(
    navController: NavHostController,
    onPlayClick: () -> Unit
) {
    navigation(
        route = "home_root",
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(onPlayClick)
        }
    }
}

fun NavGraphBuilder.shelfGraph(navController: NavHostController) {
    navigation(
        route = "shelf_root",
        startDestination = "shelf"
    ) {
        composable("shelf") {
            ShelfScreen()
        }
    }
}

fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    navigation(
        route = "settings_root",
        startDestination = "settings"
    ) {
        composable("settings") {
            SettingsScreen()
        }
    }
}

fun NavGraphBuilder.playerGraph(
    navController: NavHostController,
    viewModel: PlayerViewModel,
) {
    navigation(
        route = "player_root",
        startDestination = "player"
    ) {
        composable(
            route = "player",
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { -it / 3 },
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInVertically(
                    initialOffsetY = { -it / 3 },
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(animationSpec = tween(300,))
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            PlayerScreen(
                viewModel = viewModel,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}