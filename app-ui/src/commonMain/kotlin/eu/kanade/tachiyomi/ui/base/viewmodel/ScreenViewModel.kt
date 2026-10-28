package eu.kanade.tachiyomi.ui.base.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import cafe.adriel.voyager.core.screen.Screen
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Returns a ViewModel stored with this screen rather than the screen being composed, so screens pushed on top of it
 * share one instance with it.
 */
@Composable
inline fun <reified VM : ViewModel> Screen.rememberScreenViewModel(key: String? = null): VM =
    metroViewModel<VM>(viewModelStoreOwner = rememberScreenViewModelStoreOwner(), key = key)

/** The store of this screen's ViewModels, cleared when the screen leaves the navigator. */
@PublishedApi
@Composable
internal expect fun Screen.rememberScreenViewModelStoreOwner(): ViewModelStoreOwner
