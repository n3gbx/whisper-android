package org.n3gbx.whisper

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json
import org.n3gbx.whisper.ui.theme.WhisperTheme
import org.n3gbx.whisper.feature.player.PlayerViewModel
import org.n3gbx.whisper.ui.common.components.VerticalSlideTransitionWrapper
import org.n3gbx.whisper.ui.navigation.CatalogRoot
import org.n3gbx.whisper.ui.navigation.NavBar
import org.n3gbx.whisper.ui.navigation.NavHost
import org.n3gbx.whisper.ui.navigation.Player
import org.n3gbx.whisper.ui.navigation.SettingsRoot
import org.n3gbx.whisper.ui.navigation.LibraryRoot
import org.n3gbx.whisper.ui.navigation.Tab

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WhisperTheme {
                RootContent()
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun RootContent() {
        val playerViewModel = hiltViewModel<PlayerViewModel>()

        val navBarTabs = listOf(CatalogRoot, LibraryRoot, SettingsRoot)
        val navController = rememberNavController()
        val currentSelectedTab by navController.currentTabAsState()
        val currentDestination by navController.currentDestinationAsState()

        val isCurrentDestinationPlayer by remember(currentDestination) {
            mutableStateOf(currentDestination?.hasRoute(Player::class) ?: false)
        }

        val isNavBarVisible by rememberSaveable(currentDestination) {
            mutableStateOf(currentDestination.isTab())
        }

        val miniPlayerBottomOffsetDp by animateDpAsState(
            targetValue = if (isNavBarVisible) 80.dp else 0.dp,
            animationSpec = tween(durationMillis = 300),
            label = "miniPlayerBottomOffsetDp"
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                VerticalSlideTransitionWrapper(
                    isVisible = isNavBarVisible
                ) {
                    NavBar(
                        tabs = navBarTabs,
                        selectedTab = currentSelectedTab,
                        onTabClick = {
                            navController.navigateToRootTab(it)
                        }
                    )
                }
            },
            contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                NavHost(
                    playerViewModel = playerViewModel,
                    navController = navController,
                )

                MainMiniPlayer(
                    playerViewModel = playerViewModel,
                    shouldHide = isCurrentDestinationPlayer,
                    additionalBottomOffsetDp = miniPlayerBottomOffsetDp,
                    onClick = {
                        navController.navigate(Player())
                    }
                )
            }
        }
    }

    @Stable
    @Composable
    private fun NavController.currentTabAsState(): State<Tab> {
        val selectedItem = remember { mutableStateOf<Tab>(CatalogRoot) }

        DisposableEffect(this) {
            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
               when {
                    destination.hierarchy.any { it.hasRoute(CatalogRoot::class) } -> {
                        selectedItem.value = CatalogRoot
                    }
                    destination.hierarchy.any { it.hasRoute(LibraryRoot::class) } -> {
                        selectedItem.value = LibraryRoot
                    }
                    destination.hierarchy.any { it.hasRoute(SettingsRoot::class) } -> {
                        selectedItem.value = SettingsRoot
                    }
                }

            }
            addOnDestinationChangedListener(listener)
            onDispose {
                removeOnDestinationChangedListener(listener)
            }
        }
        return selectedItem
    }

    @Stable
    @Composable
    private fun NavController.currentDestinationAsState(): State<NavDestination?> {
        val selectedItem = remember { mutableStateOf<NavDestination?>(null) }
        DisposableEffect(this) {
            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                selectedItem.value = destination
            }
            addOnDestinationChangedListener(listener)

            onDispose {
                removeOnDestinationChangedListener(listener)
            }
        }
        return selectedItem
    }

    private fun NavController.navigateToRootTab(tab: Tab) {
        navigate(tab) {
            launchSingleTop = true
            restoreState = true
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
        }
    }

    private fun NavDestination?.isTab(): Boolean {
        if (this == null) return false
        return listOf(CatalogRoot, LibraryRoot, SettingsRoot).any { tab ->
            this.hierarchy.any { it.hasRoute(tab::class) }
        }
    }

    inline fun <reified T> Sequence<NavDestination>.getRouteAs(): T? {
        return mapNotNull { it.getRouteAs<T>() }.firstOrNull()
    }

    inline fun <reified T> NavDestination.getRouteAs(): T? {
        val routeString = this.route ?: return null
        return try {
            Json.decodeFromString(routeString)
        } catch (e: Exception) {
            null
        }
    }
}