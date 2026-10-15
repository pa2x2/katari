package mihon.presentation.kit.sheet

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Prevents a tabbed sheet's pager from moving while one or more inline editors own focus. */
@Stable
internal class TabbedSheetPagerScrollLock {
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

internal val LocalTabbedSheetPagerScrollLock =
    staticCompositionLocalOf<TabbedSheetPagerScrollLock?> { null }
