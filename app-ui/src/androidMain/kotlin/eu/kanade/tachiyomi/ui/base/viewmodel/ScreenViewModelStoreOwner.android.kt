package eu.kanade.tachiyomi.ui.base.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import cafe.adriel.voyager.androidx.AndroidScreenLifecycleOwner
import cafe.adriel.voyager.core.screen.Screen

/**
 * Voyager keeps one [AndroidScreenLifecycleOwner] per screen key and clears its store when the screen leaves the
 * navigator, so the instance lives as long as this screen's own ViewModels.
 */
@PublishedApi
@Composable
internal actual fun Screen.rememberScreenViewModelStoreOwner(): ViewModelStoreOwner = remember(this.key) {
    // The same lookup Voyager's navigator makes when it renders this screen, so both reach one store.
    AndroidScreenLifecycleOwner.get(this) as ViewModelStoreOwner
}
