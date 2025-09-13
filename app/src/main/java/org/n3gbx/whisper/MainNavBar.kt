package org.n3gbx.whisper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MainNavBar(
    modifier: Modifier = Modifier,
    tabs: List<NavTab>,
    selectedTab: NavTab,
    onTabClick: (NavTab) -> Unit,
) {
    NavigationBar(
        modifier = modifier.shadow(10.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = {
                    onTabClick(tab)
                },
                label = {
                    Text(tab.label)
                },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            is NavTab.Home -> Icons.Rounded.Home
                            is NavTab.Shelf -> Icons.Rounded.Bookmarks
                            is NavTab.Settings -> Icons.Rounded.Settings
                            else -> Icons.Default.Home
                        },
                        contentDescription = tab.label
                    )
                },
                colors =  NavigationBarItemDefaults.colors().copy(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    selectedIndicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.outline,
                    unselectedTextColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

sealed class NavTab(
    val route: String,
    val label: String,
) {
    data object Home : NavTab("home_root", "Home")
    data object Shelf : NavTab("shelf_root", "Shelf")
    data object Settings : NavTab("settings_root", "Settings")
    data object Player : NavTab("player_root", "Player")
}