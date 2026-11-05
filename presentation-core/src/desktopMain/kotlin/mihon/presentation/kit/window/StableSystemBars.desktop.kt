package mihon.presentation.kit.window

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable

// A desktop window's bars don't hide while the window is shown, so their room is the same either way.
actual val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable get() = statusBars

actual val WindowInsets.Companion.stableNavigationBars: WindowInsets
    @Composable get() = navigationBars

actual val WindowInsets.Companion.stableSystemBars: WindowInsets
    @Composable get() = systemBars
