package mihon.presentation.kit.window

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The window's full screen, for a screen that offers it itself, such as a player's full-screen button. The window
 * also enters and leaves it on its own (F11 on desktop), so [isFullScreen] can change under the screen at any time.
 * Leaving puts the window back as it was before it entered.
 */
@Stable
interface WindowFullScreen {
    /** Observable: reading it in composition recomposes when the window enters or leaves full screen. */
    val isFullScreen: Boolean

    fun enter()

    fun leave()
}

/** Null where a screen can't ask for full screen, as on Android, where screens hide the system bars instead. */
val LocalWindowFullScreen = staticCompositionLocalOf<WindowFullScreen?> { null }
