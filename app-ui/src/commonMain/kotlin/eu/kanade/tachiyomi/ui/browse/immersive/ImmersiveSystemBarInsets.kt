package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/**
 * The status bar's room even while immersive browsing hides the bar, so chrome laid over full-screen entries keeps its
 * place as the bar comes and goes.
 */
internal expect val WindowInsets.Companion.immersiveStatusBars: WindowInsets
    @Composable get

/** The navigation bar's room even while immersive browsing hides the bar, as [immersiveStatusBars] keeps the top's. */
internal expect val WindowInsets.Companion.immersiveNavigationBars: WindowInsets
    @Composable get
