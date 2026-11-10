package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.screen.ScreenKey

/**
 * Gives the root navigator's screen this is composed in the whole window while [hidden] holds, as a [FullWindowScreen]
 * has it: the side dock steps aside in a wide window and the home tabs' bottom dock in a narrow one. For a screen or home
 * tab that takes the window for a while, such as an immersive view or a selection.
 *
 * Each call holds its own request and drops it when it leaves the composition, so a tab or screen that goes away while
 * hiding them can't leave the destinations hidden for the next one.
 */
@Composable
fun HideHomeNavigation(hidden: Boolean) {
    val screen = LocalHomeNavigationScreen.current ?: return
    val state = LocalHomeNavigation.current
    DisposableEffect(state, screen, hidden) {
        if (hidden) state.hideFor(screen)
        onDispose { if (hidden) state.releaseFor(screen) }
    }
}

/** The key of the root navigator's screen being composed, which [HideHomeNavigation] hides the destinations for. */
internal val LocalHomeNavigationScreen = staticCompositionLocalOf<ScreenKey?> { null }
