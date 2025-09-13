package org.n3gbx.whisper

import android.annotation.SuppressLint
import android.content.ComponentName
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import org.n3gbx.whisper.ui.theme.WhisperTheme
import org.n3gbx.whisper.NavTab.Player
import org.n3gbx.whisper.NavTab.Home
import org.n3gbx.whisper.NavTab.Shelf
import org.n3gbx.whisper.NavTab.Settings
import org.n3gbx.whisper.feature.player.PlayerViewModel
import org.n3gbx.whisper.ui.common.components.VerticalSlideTransitionWrapper

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

        val navGraphs = listOf(Home, Shelf, Settings, Player)
        val navBarTabs = listOf(Home, Shelf, Settings)
        val navController = rememberNavController()
        val currentSelectedTab by navController.currentTabAsState()
        val currentRoute by navController.currentRouteAsState()
        val isNavBarVisible by rememberSaveable(currentRoute) { mutableStateOf(currentRoute in listOf("home", "shelf", "settings")) }

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
                    MainNavBar(
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
                MainNavHost(
                    playerViewModel = playerViewModel,
                    navController = navController,
                    onPlayClick = {

                    }
                )

                MainMiniPlayer(
                    playerViewModel = playerViewModel,
                    shouldHide = currentRoute == "player",
                    additionalBottomOffsetDp = miniPlayerBottomOffsetDp,
                    onClick = {
                        navController.navigate("player_root")
                    }
                )
            }
        }
    }

    @Stable
    @Composable
    private fun NavController.currentTabAsState(): State<NavTab> {
        val selectedItem = remember { mutableStateOf<NavTab>(Home) }

        DisposableEffect(this) {
            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                when {
                    destination.hierarchy.any { it.route == Home.route } -> {
                        selectedItem.value = Home
                    }
                    destination.hierarchy.any { it.route == Shelf.route } -> {
                        selectedItem.value = Shelf
                    }
                    destination.hierarchy.any { it.route == Settings.route } -> {
                        selectedItem.value = Settings
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
    private fun NavController.currentRouteAsState(): State<String?> {
        val selectedItem = remember { mutableStateOf<String?>(null) }
        DisposableEffect(this) {
            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                selectedItem.value = destination.route
            }
            addOnDestinationChangedListener(listener)

            onDispose {
                removeOnDestinationChangedListener(listener)
            }
        }
        return selectedItem
    }

    private fun NavController.navigateToRootTab(rootScreen: NavTab) {
        navigate(rootScreen.route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(graph.findStartDestination().id) {
                saveState = true
            }
        }
    }
}