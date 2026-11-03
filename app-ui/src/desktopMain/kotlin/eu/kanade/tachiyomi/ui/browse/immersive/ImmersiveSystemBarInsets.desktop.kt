package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable

// A desktop window's bars don't hide while the window is shown, so their room is the same either way.
internal actual val WindowInsets.Companion.immersiveStatusBars: WindowInsets
    @Composable get() = statusBars

internal actual val WindowInsets.Companion.immersiveNavigationBars: WindowInsets
    @Composable get() = navigationBars
