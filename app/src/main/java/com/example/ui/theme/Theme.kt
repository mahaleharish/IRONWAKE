package com.example.ui.theme

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
    primary = NeonGreen,
    onPrimary = TrueBlack,
    secondary = NeonCyan,
    onSecondary = TrueBlack,
    tertiary = HardcoreSteel,
    background = TrueBlack,
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = MetalGray,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = HardcoreSteel,
    onSurfaceVariant = SoftGray,
    error = BrightRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for the hardcore gym look
    dynamicColor: Boolean = false, // Keep consistent branding colors
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
