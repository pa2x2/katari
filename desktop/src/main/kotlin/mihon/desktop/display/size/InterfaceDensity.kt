package mihon.desktop.display.size

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import tachiyomi.presentation.core.util.collectAsState

/**
 * Gives [content] the session's scale times the interface size as its density, in place of the window's, which is
 * the toolkit's whole-number scale where the session's scale is left to the app.
 */
@Composable
fun ProvideInterfaceDensity(
    sessionScale: SessionScale,
    preferences: InterfaceSizePreferences,
    content: @Composable () -> Unit,
) {
    val windowDensity = LocalDensity.current
    val percent by preferences.interfaceSize.collectAsState()
    val density = remember(windowDensity, percent) {
        val pixelsPerDp = sessionScale.pixelsPerDp ?: windowDensity.density
        Density(pixelsPerDp * percent.coerceIn(InterfaceSize.RANGE) / PERCENT, windowDensity.fontScale)
    }
    CompositionLocalProvider(LocalDensity provides density, content = content)
}

private const val PERCENT = 100f
