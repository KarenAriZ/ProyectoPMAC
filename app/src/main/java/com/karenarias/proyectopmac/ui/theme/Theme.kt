package com.karenarias.proyectopmac.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    secondary = MauveAccent,
    tertiary = DustyRose,
    background = PurpleDark,
    surface = PurpleDark,
    error = ErrorWine
)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = CardWhite,
    primaryContainer = MauveAccent,
    onPrimaryContainer = PurpleDark,
    secondary = DustyRose,
    background = BackgroundCream,
    surface = CardWhite,
    onBackground = PurpleDark,
    onSurface = PurpleDark,
    error = ErrorWine,
    onError = CardWhite
)

@Composable
fun ProyectoPMACTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default to respect the custom vintage palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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
        content = content
    )
}
