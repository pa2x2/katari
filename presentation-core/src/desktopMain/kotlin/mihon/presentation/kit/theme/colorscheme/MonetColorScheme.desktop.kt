package mihon.presentation.kit.theme.colorscheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import mihon.presentation.kit.theme.rememberKatariThemeGraph

// Seeded from the desktop's accent colour, as Android 8.1 to 11 seeds it from the wallpaper's, and followed as the
// accent changes. Without an accent Dynamic looks like the default theme, as on Android before 8.1.
@Composable
internal actual fun rememberMonetColorScheme(): () -> BaseColorScheme {
    val accent by rememberKatariThemeGraph().systemAccentColor.accentArgb.collectAsState()
    return remember(accent) { { accent?.let { MonetCompatColorScheme(Color(it)) } ?: TachiyomiColorScheme } }
}
