package mihon.presentation.kit.pointer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the mouse pointer is over one of the app's windows, for what a mouse keeps up only while it is around, such
 * as the player's controls, which go the moment it leaves the app rather than after resting a while.
 */
@Stable
interface AppPointerPresence {
    /** Observable: reading it in composition or a snapshot flow follows the pointer in and out of the app. */
    val isOverApp: Boolean
}

/** Null where windows have no pointer that comes and goes, as on Android, where they are worked by touch. */
val LocalAppPointerPresence = staticCompositionLocalOf<AppPointerPresence?> { null }
