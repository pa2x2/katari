package app.katari.presentation.kit.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Content that fills the whole window rather than sitting beside the floating docks, such as an immersive feed, a
 * story or a viewer: while it is shown, every [FloatingDockHost] around the screen it is in hides its dock, and neither
 * it nor a sheet or dialog opened from it keeps room for one. Build any view that draws edge to edge in it, so the
 * docks step aside by themselves instead of each such view having to ask.
 */
@Composable
fun FullWindowContent(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    HideDocks(hidden = true)
    CompositionLocalProvider(
        LocalDockClearance provides DockClearance.None,
        LocalDockOnScreen provides DockClearance.None,
    ) {
        Box(modifier = modifier, content = content)
    }
}

/**
 * Hides the floating docks around the screen this is composed in while [hidden] holds, for a screen that keeps its own
 * layout but takes the window for a while, such as the library during a selection, whose action bar takes the dock's
 * place. Content that fills the window goes in [FullWindowContent] instead.
 */
@Composable
fun HideDocks(hidden: Boolean) {
    val requests = LocalDockHideRequests.current ?: return
    DisposableEffect(requests, hidden) {
        if (hidden) requests.add()
        onDispose { if (hidden) requests.remove() }
    }
}

/**
 * The requests to hide the docks made from the content of one [FloatingDockHost] or one [DockedScreen]. A host's
 * requests also count towards the screen or host it is nested in, so a home tab's request hides the side dock beside
 * home as well as the tabs' own bottom dock.
 */
@Stable
internal class DockHideRequests(private val outer: DockHideRequests?) {
    private var count by mutableIntStateOf(0)

    val any: Boolean get() = count > 0

    fun add() {
        count++
        outer?.add()
    }

    fun remove() {
        count--
        outer?.remove()
    }
}

internal val LocalDockHideRequests = compositionLocalOf<DockHideRequests?> { null }
