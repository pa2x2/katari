package tachiyomi.presentation.core.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Prevents a viewer-settings pager from moving while one or more inline editors own focus. */
@Stable
internal class ViewerSettingsPagerScrollLock {
    private val owners = mutableStateMapOf<Any, Unit>()

    val isLocked: Boolean
        get() = owners.isNotEmpty()

    fun acquire(owner: Any) {
        owners[owner] = Unit
    }

    fun release(owner: Any) {
        owners.remove(owner)
    }
}

internal val LocalViewerSettingsPagerScrollLock =
    staticCompositionLocalOf<ViewerSettingsPagerScrollLock?> { null }
