package app.katari.presentation.kit.window

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.runtime.Composable

actual val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable get() = statusBarsIgnoringVisibility

actual val WindowInsets.Companion.stableNavigationBars: WindowInsets
    @Composable get() = navigationBarsIgnoringVisibility

actual val WindowInsets.Companion.stableSystemBars: WindowInsets
    @Composable get() = systemBarsIgnoringVisibility
