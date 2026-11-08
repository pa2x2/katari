package app.katari.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.katari.core.metro.metroGraph

actual interface PlatformKatariThemeGraph : KatariThemeGraph

@Composable
internal actual fun rememberKatariThemeGraph(): PlatformKatariThemeGraph {
    LocalKatariThemeGraph.current?.let { return it }
    val context = LocalContext.current
    return remember(context) { context.metroGraph<PlatformKatariThemeGraph>() }
}
