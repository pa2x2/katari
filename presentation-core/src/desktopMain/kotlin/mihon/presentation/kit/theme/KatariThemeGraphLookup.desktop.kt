package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/** Provided by the desktop application around each window's content, as Android reaches its graph from a Context. */
val LocalKatariThemeGraph: ProvidableCompositionLocal<KatariThemeGraph> = staticCompositionLocalOf {
    error("No KatariThemeGraph provided around this window's content")
}

@Composable
internal actual fun rememberKatariThemeGraph(): KatariThemeGraph = LocalKatariThemeGraph.current
