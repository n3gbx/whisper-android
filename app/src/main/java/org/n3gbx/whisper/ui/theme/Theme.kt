package org.n3gbx.whisper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val lightColorScheme = lightColorScheme(
    primary = Color(0xFFFF5722),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFF5722).copy(alpha = 0.8f),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF121212),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF121212).copy(alpha = 0.9f),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFFAB00),
    onTertiary = Color(0xFF121212),
    tertiaryContainer = Color(0xFFFFAB00).copy(alpha = 0.9f),
    onTertiaryContainer = Color(0xFF121212),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF121212),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCD8DF),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFBDBDBD),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF121212),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFFF5722)
)

private val darkColorScheme = darkColorScheme(
    primary = Color(0xFFFF7043),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFFF7043).copy(alpha = 0.8f),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFE0E0E0).copy(alpha = 0.2f),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFFFFD740),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFFFFD740).copy(alpha = 0.2f),
    onTertiaryContainer = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD4),
    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFF373737),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE0E0E0),
    inverseOnSurface = Color(0xFF121212),
    inversePrimary = Color(0xFFFF7043)
)

@Composable
fun WhisperTheme(
    isSystemInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isSystemInDarkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}