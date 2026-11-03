package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable

internal actual val WindowInsets.Companion.immersiveStatusBars: WindowInsets
    @Composable get() = statusBarsIgnoringVisibility

internal actual val WindowInsets.Companion.immersiveNavigationBars: WindowInsets
    @Composable get() = navigationBarsIgnoringVisibility
