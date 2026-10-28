package eu.kanade.tachiyomi.ui.base.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import cafe.adriel.voyager.core.lifecycle.ScreenDisposable
import cafe.adriel.voyager.core.lifecycle.ScreenLifecycleStore
import cafe.adriel.voyager.core.screen.Screen

/**
 * Voyager keeps no ViewModel store per screen on desktop, so one is registered with the screen's lifecycle store,
 * which disposes it, and clears its ViewModels, when the screen leaves the navigator.
 */
@PublishedApi
@Composable
internal actual fun Screen.rememberScreenViewModelStoreOwner(): ViewModelStoreOwner = remember(this.key) {
    ScreenLifecycleStore.get(this) { ScreenViewModelStore() }
}

private class ScreenViewModelStore :
    ViewModelStoreOwner,
    ScreenDisposable {
    override val viewModelStore = ViewModelStore()

    override fun onDispose(screen: Screen) = viewModelStore.clear()
}
