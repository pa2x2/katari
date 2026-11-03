package mihon.presentation.kit.window

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset

/**
 * Where the window was last touched, so a dialog or sheet can open on the side of a half-open fold the user is acting
 * on without each caller saying where it was opened from. Plain fields rather than state: it is read once, as a
 * surface opens, and nothing should recompose on every touch.
 */
internal class LastTouch {
    /** In window pixels; null until the first touch. */
    var positionInWindow: Offset? = null
}

/** Null where the platform records no touches, which leaves surfaces on their default side. */
internal val LocalLastTouch = staticCompositionLocalOf<LastTouch?> { null }
