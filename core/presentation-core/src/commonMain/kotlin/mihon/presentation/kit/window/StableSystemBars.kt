package mihon.presentation.kit.window

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/**
 * The status bar's room even while it is hidden, as when a reader or immersive browsing hides the system bars, so
 * chrome laid over full-screen content keeps its place as the bar comes and goes.
 */
expect val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable get

/** The navigation bar's room even while it is hidden, as [stableStatusBars] keeps the top's. */
expect val WindowInsets.Companion.stableNavigationBars: WindowInsets
    @Composable get

/** Every system bar's room even while hidden, including a free-form window's caption bar, as [stableStatusBars]. */
expect val WindowInsets.Companion.stableSystemBars: WindowInsets
    @Composable get
