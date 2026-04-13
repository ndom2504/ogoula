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

private val DarkColorScheme = darkColorScheme(
    primary = GreenGabo,
    onPrimary = OgoulaWhite,
    primaryContainer = BlueGabo,
    onPrimaryContainer = OgoulaWhite,
    secondary = YellowGabo,
    onSecondary = OgoulaWhite,
    tertiary = Color(0xFF6B9088),
    onTertiary = OgoulaWhite,
    background = Color(0xFF121E1B),
    onBackground = Color(0xFFE8F0ED),
    surface = Color(0xFF182824),
    onSurface = Color(0xFFE8F0ED),
    surfaceVariant = Color(0xFF243530),
    onSurfaceVariant = Color(0xFFB8C9C3),
)

private val LightColorScheme = lightColorScheme(
    primary = GreenGabo,
    onPrimary = OgoulaWhite,
    primaryContainer = OgoulaSurfaceTint,
    onPrimaryContainer = BlueGabo,
    secondary = YellowGabo,
    onSecondary = OgoulaWhite,
    tertiary = Color(0xFF6B8580),
    onTertiary = OgoulaWhite,
    background = OgoulaWhite,
    onBackground = Color(0xFF1A2220),
    surface = OgoulaWhite,
    onSurface = Color(0xFF1A2220),
    surfaceVariant = OgoulaSurfaceTint,
    onSurfaceVariant = Color(0xFF3D524C),
    outline = Color(0xFFC5D5CF),
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
