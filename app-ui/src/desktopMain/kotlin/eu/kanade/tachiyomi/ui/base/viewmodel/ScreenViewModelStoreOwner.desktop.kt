package eu.kanade.tachiyomi.ui.base.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStoreOwner
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.tachiyomi.ui.base.lifecycle.DesktopScreenLifecycleOwner

/** The owner the window's navigators render this screen with, so both reach one store. */
@PublishedApi
@Composable
internal actual fun Screen.rememberScreenViewModelStoreOwner(): ViewModelStoreOwner = remember(this.key) {
    DesktopScreenLifecycleOwner.get(this)
}
