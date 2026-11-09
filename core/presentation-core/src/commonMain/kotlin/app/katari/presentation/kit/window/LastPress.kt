package app.katari.presentation.kit.window

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Where the window was last pressed, by a touch or a mouse button, so a dialog or sheet can open beside what opened it
 * (the half of a half-open fold, the edge of a wide window) without each caller saying where that was. A key press
 * clears it, since a surface opened from the keyboard was opened from no point. Plain fields rather than state: it is
 * read once, as a surface opens, and nothing should recompose on every press.
 */
internal class LastPress {
    /** In window pixels; null until the first press and after a key press. */
    var positionInWindow: Offset? = null
        private set

    private var pressedAt: TimeSource.Monotonic.ValueTimeMark? = null

    fun press(position: Offset) {
        positionInWindow = position
        pressedAt = TimeSource.Monotonic.markNow()
    }

    fun keyPressed() {
        positionInWindow = null
        pressedAt = null
    }

    /**
     * The last press if it just happened, so it is what opened a surface opening now. A surface that opens on its own
     * a while later, such as an update offer, was opened by nothing the user pressed.
     */
    val openingPositionInWindow: Offset?
        get() = positionInWindow?.takeIf { pressedAt?.elapsedNow()?.let { it < OpeningPressWindow } == true }
}

internal val LocalLastPress = staticCompositionLocalOf<LastPress?> { null }

// A surface opens a frame or two after its button is released, or after a short load (a sheet waiting for its data)
private val OpeningPressWindow = 3.seconds
