package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * [KatariThemeGraph] on desktop, where the theme also reads the desktop's own light or dark preference instead of
 * AppCompat's, its accent colour instead of the wallpaper's, and its interface font.
 */
interface DesktopKatariThemeGraph : KatariThemeGraph {
    val systemColorScheme: SystemColorScheme
    val systemAccentColor: SystemAccentColor
    val systemInterfaceFont: SystemInterfaceFont
}

/** Provided by the desktop application around each window's content, as Android reaches its graph from a Context. */
val LocalKatariThemeGraph: ProvidableCompositionLocal<DesktopKatariThemeGraph> = staticCompositionLocalOf {
    error("No KatariThemeGraph provided around this window's content")
}

@Composable
internal actual fun rememberKatariThemeGraph(): KatariThemeGraph = LocalKatariThemeGraph.current
