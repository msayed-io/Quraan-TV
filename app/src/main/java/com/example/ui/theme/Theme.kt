package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppleDarkColorScheme = darkColorScheme(
    primary = AppleTextPrimary,
    onPrimary = AppleBaseBackground,
    primaryContainer = AppleTertiaryBackground,
    onPrimaryContainer = AppleTextPrimary,
    secondary = AppleTextSecondary,
    onSecondary = AppleBaseBackground,
    secondaryContainer = AppleQuaternaryBackground,
    onSecondaryContainer = AppleTextPrimary,
    background = AppleBaseBackground,
    onBackground = AppleTextPrimary,
    surface = AppleSecondaryBackground,
    onSurface = AppleTextPrimary,
    surfaceVariant = AppleTertiaryBackground,
    onSurfaceVariant = AppleTextSecondary,
    outline = AppleSubtleBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppleDarkColorScheme,
        typography = Typography,
        content = content
    )
}
