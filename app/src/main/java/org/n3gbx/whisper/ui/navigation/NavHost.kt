package org.n3gbx.whisper.ui.navigation

import android.os.Bundle
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.n3gbx.whisper.feature.catalog.CatalogScreen
import org.n3gbx.whisper.feature.player.PlayerScreen
import org.n3gbx.whisper.feature.player.PlayerViewModel
import org.n3gbx.whisper.feature.settings.SettingsScreen
import org.n3gbx.whisper.feature.library.LibraryScreen
import org.n3gbx.whisper.model.Identifier
import kotlin.reflect.typeOf

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
            typeMap = mapOf(
                typeOf<Identifier?>() to IdentifierNavType
            ),
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

object IdentifierNavType : NavType<Identifier?>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): Identifier? {
        return bundle.getString(key)?.let { parseValue(it) }
    }

    override fun put(bundle: Bundle, key: String, value: Identifier?) {
        bundle.putString(key, serializeAsValue(value))
    }

    override fun parseValue(value: String): Identifier {
        return Json.decodeFromString(value)
    }

    override fun serializeAsValue(value: Identifier?): String {
        return Json.encodeToString(value)
    }
}