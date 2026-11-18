package com.example.time.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ReelGuardColorScheme = darkColorScheme(
    primary = AccentMint,
    onPrimary = OnAccentMint,
    primaryContainer = AccentMintContainer,
    onPrimaryContainer = AccentMint,
    background = CharcoalBackground,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CharcoalSurfaceBorder
)

@Composable
fun TimeTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ReelGuardColorScheme,
        typography = Typography,
        content = content
    )
}
