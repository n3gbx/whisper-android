package org.n3gbx.whisper.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import org.n3gbx.whisper.feature.catalog.CatalogScreen
import org.n3gbx.whisper.feature.player.PlayerScreen
import org.n3gbx.whisper.feature.player.PlayerViewModel
import org.n3gbx.whisper.feature.settings.SettingsScreen
import org.n3gbx.whisper.feature.library.LibraryScreen

@ExperimentalFoundationApi
@Composable
fun NavHost(
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel,
    navController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = CatalogRoot,
        enterTransition = { fadeIn(animationSpec = tween(0)) },
        exitTransition = { fadeOut(animationSpec = tween(0)) }
    ) {
        catalogGraph(navController)
        libraryGraph(navController)
        settingsGraph(navController)
        playerGraph(navController, playerViewModel)
    }
}

fun NavGraphBuilder.catalogGraph(
    navController: NavHostController,
) {
    navigation<CatalogRoot>(
        startDestination = Catalog
    ) {
        composable<Catalog> {
            CatalogScreen(
                viewModel = hiltViewModel(),
                navigateToPlayer = { bookId ->
                    navController.navigate(Player(bookId))
                }
            )
        }
    }
}

fun NavGraphBuilder.libraryGraph(
    navController: NavHostController
) {
    navigation<LibraryRoot>(
        startDestination = Library
    ) {
        composable<Library> {
            LibraryScreen(
                viewModel = hiltViewModel(),
                navigateToPlayer = { bookId ->
                    navController.navigate(Player(bookId))
                }
            )
        }
    }
}

fun NavGraphBuilder.settingsGraph(
    navController: NavHostController
) {
    navigation<SettingsRoot>(
        startDestination = Settings
    ) {
        composable<Settings> {
            SettingsScreen()
        }
    }
}

fun NavGraphBuilder.playerGraph(
    navController: NavHostController,
    viewModel: PlayerViewModel,
) {
    navigation<PlayerRoot>(
        startDestination = Player()
    ) {
        composable<Player>(
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
        ) { backStackEntry ->
            val bookId = backStackEntry.toRoute<Player>().bookId
            PlayerScreen(
                viewModel = viewModel,
                bookId = bookId,
                navigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}