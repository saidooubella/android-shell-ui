package com.example.demo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.luminance
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

private fun Color.isDark(): Boolean {
    return toArgb().luminance < .5f
}

@Composable
internal fun DemoTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {

    val colorScheme = when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        true -> when (darkTheme) {
            true -> dynamicDarkColorScheme(LocalContext.current)
            else -> dynamicLightColorScheme(LocalContext.current)
        }
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightNavigationBars = colorScheme.primary.isDark()
            controller.isAppearanceLightStatusBars = colorScheme.primary.isDark()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme.animate(),
        typography = Typography,
        content = content
    )
}

@Composable
private fun ColorScheme.animate(): ColorScheme {
    return ColorScheme(
        animateColorAsState(primary).value,
        animateColorAsState(onPrimary).value,
        animateColorAsState(primaryContainer).value,
        animateColorAsState(onPrimaryContainer).value,
        animateColorAsState(inversePrimary).value,
        animateColorAsState(secondary).value,
        animateColorAsState(onSecondary).value,
        animateColorAsState(secondaryContainer).value,
        animateColorAsState(onSecondaryContainer).value,
        animateColorAsState(tertiary).value,
        animateColorAsState(onTertiary).value,
        animateColorAsState(tertiaryContainer).value,
        animateColorAsState(onTertiaryContainer).value,
        animateColorAsState(background).value,
        animateColorAsState(onBackground).value,
        animateColorAsState(surface).value,
        animateColorAsState(onSurface).value,
        animateColorAsState(surfaceVariant).value,
        animateColorAsState(onSurfaceVariant).value,
        animateColorAsState(surfaceTint).value,
        animateColorAsState(inverseSurface).value,
        animateColorAsState(inverseOnSurface).value,
        animateColorAsState(error).value,
        animateColorAsState(onError).value,
        animateColorAsState(errorContainer).value,
        animateColorAsState(onErrorContainer).value,
        animateColorAsState(outline).value,
        animateColorAsState(outlineVariant).value,
        animateColorAsState(scrim).value,
    )
}
