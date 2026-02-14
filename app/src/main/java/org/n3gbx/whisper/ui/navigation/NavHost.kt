package org.n3gbx.whisper.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navigation
import androidx.navigation.toRoute
import org.n3gbx.whisper.feature.catalog.CatalogScreen
import org.n3gbx.whisper.feature.downloads.DownloadsScreen
import org.n3gbx.whisper.feature.library.LibraryScreen
import org.n3gbx.whisper.feature.player.PlayerScreen
import org.n3gbx.whisper.feature.player.PlayerViewModel
import org.n3gbx.whisper.feature.settings.SettingsScreen
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
        startDestination = Catalog,
        enterTransition = { tabEnterTransition() },
        exitTransition = { tabExitTransition() }
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
        startDestination = Library,
        enterTransition = { tabEnterTransition() },
        exitTransition = { tabExitTransition() }
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
        startDestination = Settings,
        enterTransition = { tabEnterTransition() },
        exitTransition = { tabExitTransition() }
    ) {
        composable<Settings> {
            val context = LocalContext.current

            SettingsScreen(
                navigateToDownloads = { navController.navigate(Downloads) },
                navigateToBrowser = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                },
                restart = { restartApplication(context) }
            )
        }
        composable<Downloads> {
            DownloadsScreen(
                navigateBack = { navController.popBackStack() }
            )
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
            typeMap = mapOf(typeOf<Identifier?>() to IdentifierNavType),
            enterTransition = { playerEnterTransition() },
            exitTransition = { playerExitTransition() },
            popExitTransition = { playerPopExitTransition() },
            popEnterTransition = { playerPopEnterTransition() }
        ) { backStackEntry ->
            // always null if comes from MiniPlayer
            val bookId = backStackEntry.toRoute<Player>().bookId

            PlayerScreen(
                viewModel = viewModel,
                bookId = bookId,
                navigateBack = { navController.popBackStack() }
            )
        }
    }
}

private fun restartApplication(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)

    intent?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(this)
    }

    Runtime.getRuntime().exit(0)
}
