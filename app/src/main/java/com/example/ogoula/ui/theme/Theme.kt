package com.example.ogoula.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Surfaces légèrement différentes du fond pour le relief (mode clair « vert + blanc »). */
private val LightGreenSurface = Color(0xFF1A5A4A)
private val LightGreenSurfaceVariant = Color(0xFF236F5C)

private val DarkColorScheme = darkColorScheme(
    primary = XBlue,
    onPrimary = XWhite,
    primaryContainer = XDarkGray,
    onPrimaryContainer = XWhite,
    secondary = XTextGray,
    onSecondary = XWhite,
    tertiary = XBlue,
    onTertiary = XWhite,
    background = XBlack,
    onBackground = XWhite,
    surface = XBlack,
    onSurface = XWhite,
    surfaceVariant = XDarkGray,
    onSurfaceVariant = XTextGray,
    outline = XBorderGray,
    outlineVariant = XBorderGray,
    // Menus / popups Material3 utilisent surfaceContainer*
    surfaceContainerLowest = XBlack,
    surfaceContainerLow = XDarkGray,
    surfaceContainer = XDarkGray,
    surfaceContainerHigh = Color(0xFF1F2937),
    surfaceContainerHighest = Color(0xFF2F3336),
)

private val LightColorScheme = lightColorScheme(
    primary = XBlue,
    onPrimary = XWhite,
    primaryContainer = XDarkGray,
    onPrimaryContainer = XWhite,
    secondary = XTextGray,
    onSecondary = XWhite,
    tertiary = XBlue,
    onTertiary = XWhite,
    background = XBlack,
    onBackground = XWhite,
    surface = XBlack,
    onSurface = XWhite,
    surfaceVariant = XDarkGray,
    onSurfaceVariant = XTextGray,
    outline = XBorderGray,
    outlineVariant = XBorderGray,
    error = Color(0xFFF4212E),
    onError = XWhite,
    errorContainer = XDarkGray,
    onErrorContainer = XWhite,
    surfaceContainerLowest = XBlack,
    surfaceContainerLow = XDarkGray,
    surfaceContainer = XDarkGray,
    surfaceContainerHigh = Color(0xFF1F2937),
    surfaceContainerHighest = Color(0xFF2F3336),
)

@Composable
fun OgoulaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
