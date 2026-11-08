package com.kizqr.app.ui.theme

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
import com.kizqr.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFFF97316),
    onPrimary = Color(0xFF3D1D00),
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF341100),
    secondary = Color(0xFF8B5E3B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF341100),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF201B15),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF201B15),
    surfaceVariant = Color(0xFFF3E0D2),
    onSurfaceVariant = Color(0xFF52443A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB874),
    onPrimary = Color(0xFF4E2600),
    primaryContainer = Color(0xFF6F3A00),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFFFB98D),
    onSecondary = Color(0xFF4F2600),
    secondaryContainer = Color(0xFF6E3D00),
    onSecondaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF1B1310),
    onBackground = Color(0xFFEFE1D8),
    surface = Color(0xFF1B1310),
    onSurface = Color(0xFFEFE1D8),
    surfaceVariant = Color(0xFF41322A),
    onSurfaceVariant = Color(0xFFD4C4B9),
)

@Composable
fun KizQrTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = KizQrTypography,
        content = content,
    )
}
