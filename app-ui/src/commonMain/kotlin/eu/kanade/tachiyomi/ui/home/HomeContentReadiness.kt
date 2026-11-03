package eu.kanade.tachiyomi.ui.home

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Told by a home tab once it has its first content to show, so whatever covers the window while the app starts, such
 * as Android's splash screen, can go.
 */
fun interface HomeContentReadiness {
    fun onContentReady()
}

val LocalHomeContentReadiness = staticCompositionLocalOf<HomeContentReadiness> {
    error("HomeContentReadiness is provided by what hosts the home screen")
}
