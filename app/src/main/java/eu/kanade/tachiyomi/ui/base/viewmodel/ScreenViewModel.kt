package eu.kanade.tachiyomi.ui.base.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.androidx.AndroidScreenLifecycleOwner
import cafe.adriel.voyager.core.screen.Screen

/**
 * Returns a ViewModel stored with this screen rather than the screen being composed, so screens pushed on top of it
 * share one instance with it. Voyager keeps one [AndroidScreenLifecycleOwner] per screen key and clears its store
 * when the screen leaves the navigator, so the instance lives as long as this screen's own ViewModels.
 */
@Composable
inline fun <reified VM : ViewModel> Screen.rememberScreenViewModel(
    key: String? = null,
    noinline initializer: CreationExtras.() -> VM,
): VM {
    val owner = remember(this.key) {
        // The same lookup Voyager's navigator makes when it renders this screen, so both reach one store.
        AndroidScreenLifecycleOwner.get(this) as ViewModelStoreOwner
    }
    return viewModel(viewModelStoreOwner = owner, key = key, initializer = initializer)
}
