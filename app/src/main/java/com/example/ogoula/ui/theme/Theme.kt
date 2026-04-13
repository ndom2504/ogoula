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
    primary = GreenGabo,
    onPrimary = OgoulaWhite,
    primaryContainer = BlueGabo,
    onPrimaryContainer = OgoulaWhite,
    secondary = YellowGabo,
    onSecondary = Color(0xFF1A1A1A),
    tertiary = Color(0xFF6B9088),
    onTertiary = OgoulaWhite,
    background = Color(0xFF121E1B),
    onBackground = Color(0xFFE8F0ED),
    surface = Color(0xFF182824),
    onSurface = Color(0xFFE8F0ED),
    surfaceVariant = Color(0xFF243530),
    onSurfaceVariant = Color(0xFFB8C9C3),
)

/**
 * Mode clair : fonds verts, textes blancs (identité historique Ogoula).
 * Boutons principaux : pastille blanche, texte vert (#1C745E).
 */
private val LightColorScheme = lightColorScheme(
    primary = OgoulaWhite,
    onPrimary = GreenGabo,
    primaryContainer = OgoulaSurfaceTint,
    onPrimaryContainer = GreenGabo,
    secondary = Color(0xFFB5DFD0),
    onSecondary = GreenGabo,
    tertiary = Color(0xFF8FC9B8),
    onTertiary = BlueGabo,
    background = GreenGabo,
    onBackground = OgoulaWhite,
    surface = LightGreenSurface,
    onSurface = OgoulaWhite,
    surfaceVariant = LightGreenSurfaceVariant,
    onSurfaceVariant = OgoulaWhite.copy(alpha = 0.88f),
    outline = OgoulaWhite.copy(alpha = 0.38f),
    outlineVariant = OgoulaWhite.copy(alpha = 0.22f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
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
