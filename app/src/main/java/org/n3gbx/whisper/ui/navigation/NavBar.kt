package org.n3gbx.whisper.ui.navigation

import androidx.compose.material.icons.Icons
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
fun NavBar(
    modifier: Modifier = Modifier,
    tabs: List<Tab>,
    selectedTab: Tab,
    onTabClick: (Tab) -> Unit,
) {
    NavigationBar(
        modifier = modifier.shadow(10.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val icon = when (tab) {
                is CatalogRoot -> Icons.Rounded.Home
                is LibraryRoot -> Icons.Rounded.Bookmarks
                is SettingsRoot -> Icons.Rounded.Settings
            }
            val label = when (tab) {
                is CatalogRoot -> "Catalog"
                is LibraryRoot -> "Library"
                is SettingsRoot -> "Settings"
            }

            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = {
                    onTabClick(tab)
                },
                label = {
                    Text(text = label)
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
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