package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * [KatariThemeGraph] with what the platform's theme also reads from the app graph: on desktop, the desktop's own light
 * or dark preference instead of AppCompat's, its accent colour instead of the wallpaper's, and its interface font.
 */
expect interface PlatformKatariThemeGraph : KatariThemeGraph

/**
 * Provided by the app's composition root. On Android, compositions outside it, such as the readers' and players',
 * reach the graph from their Context instead.
 */
val LocalKatariThemeGraph: ProvidableCompositionLocal<PlatformKatariThemeGraph?> = staticCompositionLocalOf { null }

/** The app graph [KatariTheme] reads the user's theme from. */
@Composable
internal expect fun rememberKatariThemeGraph(): PlatformKatariThemeGraph
