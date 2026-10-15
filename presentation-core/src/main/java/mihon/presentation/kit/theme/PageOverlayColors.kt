package mihon.presentation.kit.theme

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color

/**
 * Colours a reader or player lays over its pages or video because a setting asks for them rather than because the
 * theme picks them: a filter colour the user mixed, the shade that takes brightness below the screen's minimum, and
 * the flashes that clear an e-ink screen. They stay the same in every theme.
 */
object PageOverlayColors {
    /** The custom filter colour the user mixed, stored as an ARGB integer. */
    fun filter(@ColorInt argb: Int): Color = Color(argb)

    /** Darkens pages or video by [amount], from unchanged at 0 to black at 1. */
    fun dim(@FloatRange(from = 0.0, to = 1.0) amount: Float): Color = Color.Black.copy(alpha = amount)

    /** E-ink refresh flashes; the panel needs pure black and white to clear ghosting. */
    val flashDark: Color = Color.Black
    val flashLight: Color = Color.White
}
