package eu.kanade.tachiyomi.ui.base.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Returns a ViewModel stored with this screen rather than the screen being composed, so screens pushed on top of it
 * share one instance with it.
 */
@Composable
inline fun <reified VM : ViewModel> Screen.rememberScreenViewModel(key: String? = null): VM =
    metroViewModel<VM>(viewModelStoreOwner = rememberScreenViewModelStoreOwner(), key = key)

/**
 * Returns a ViewModel stored with the [S] this screen's navigator holds, so the screens a page opens share one instance
 * with the page. A screen shown without its page, such as one opened on its own from a reader, stores its own.
 */
@Composable
inline fun <reified S : Screen, reified VM : ViewModel> Screen.rememberPageViewModel(): VM {
    val navigator = LocalNavigator.currentOrThrow
    // Kept for the screen's whole stay: while it leaves together with its page, the page is no longer in the stack
    val page = remember(navigator) { navigator.items.lastOrNull { it is S } ?: this }
    return page.rememberScreenViewModel<VM>()
}

/** The store of this screen's ViewModels, cleared when the screen leaves the navigator. */
@PublishedApi
@Composable
internal expect fun Screen.rememberScreenViewModelStoreOwner(): ViewModelStoreOwner
