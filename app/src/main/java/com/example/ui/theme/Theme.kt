package com.example.ui.theme

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
    primary = RastaDarkPrimary,
    onPrimary = RastaDarkOnPrimary,
    primaryContainer = RastaDarkPrimaryContainer,
    onPrimaryContainer = RastaDarkOnPrimaryContainer,
    secondary = RastaDarkSecondary,
    onSecondary = RastaDarkOnSecondary,
    tertiary = RastaCyan,
    background = RastaDarkBackground,
    onBackground = RastaDarkOnBackground,
    surface = RastaDarkSurface,
    onSurface = RastaDarkOnSurface,
    surfaceVariant = RastaDarkSurfaceVariant,
    onSurfaceVariant = RastaDarkOnSurfaceVariant,
    error = RastaError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = RastaLightPrimary,
    onPrimary = RastaLightOnPrimary,
    primaryContainer = RastaLightPrimaryContainer,
    onPrimaryContainer = RastaLightOnPrimaryContainer,
    secondary = RastaLightSecondary,
    onSecondary = Color.White,
    tertiary = RastaCyan,
    background = RastaLightBg,
    onBackground = Color(0xFF0F172A),
    surface = RastaLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = RastaLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    error = RastaError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to futuristic dark theme for AI assistant
    dynamicColor: Boolean = false, // Keep consistent cyber branded palette
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
