package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mihon.core.metro.metroGraph

@Composable
internal actual fun rememberKatariThemeGraph(): KatariThemeGraph {
    val context = LocalContext.current
    return remember(context) { context.metroGraph<KatariThemeGraph>() }
}
