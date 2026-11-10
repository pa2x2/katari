package app.katari.presentation.kit.pointer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Tells the mouse being moved from the mouse being used where it is, for a media viewer that hides the pointer while it
 * rests ([HiddenPointer]) and shows it when the mouse moves, such as the player and the readers. Scrolling and clicking
 * don't move the pointer, and two things that come as moves aren't: the window repeats the last move whenever what lies
 * under a still pointer is laid out anew, and a mouse shifts a pixel or two as its wheel turns or its button clicks. So
 * while the pointer rests, a move counts only once it has gone a few dp from where the pointer last moved to.
 */
class PointerMoves {
    private var movedTo = Offset.Unspecified

    /** Whether the pointer coming to [position] is a move; when it is, the next is measured from [position]. */
    fun isMove(position: Offset, resting: Boolean, density: Density): Boolean {
        if (movedTo.isSpecified) {
            if (position == movedTo) return false
            if (resting && (position - movedTo).getDistance() <= with(density) { RestingSlop.toPx() }) return false
        }
        movedTo = position
        return true
    }
}

private val RestingSlop = 6.dp
