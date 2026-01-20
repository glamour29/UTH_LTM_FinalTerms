package com.example.client.view.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    secondary = TealLight,
    tertiary = Pink80,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkElevated,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    primaryContainer = SurfaceDarkElevated,
    onPrimaryContainer = TealLight,
    secondaryContainer = MyMessageBubbleDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiaryContainer = OtherMessageBubbleDark,
    onTertiaryContainer = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    secondary = TealAccent,
    tertiary = Pink40,
    background = BackgroundLight,
    surface = BackgroundWhite,
    surfaceVariant = SurfaceGray,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    primaryContainer = TealVeryLight,
    onPrimaryContainer = TealPrimary,
    secondaryContainer = MyMessageBubble,
    onSecondaryContainer = TextPrimary
)

@Composable
fun ClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Tắt dynamic color để dùng màu custom
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
