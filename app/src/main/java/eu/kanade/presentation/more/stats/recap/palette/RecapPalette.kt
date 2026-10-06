package eu.kanade.presentation.more.stats.recap.palette

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.materialkolor.hct.Hct

/**
 * A page's colours. Every role sits at a fixed tone, so text stays readable on the background whatever cover the
 * hue came from; only hue and colourfulness follow the cover.
 */
@Immutable
data class RecapPalette(
    val background: Color,
    val backgroundEnd: Color,
    val ink: Color,
    val accent: Color,
    val muted: Color,
    /** Faint marks on the background, such as empty calendar days and bar tracks. */
    val faint: Color,
) {
    companion object {
        /** The recap's own colours, a deep violet, for what points to a recap outside its story. */
        val Default: RecapPalette = fromSeed(0xFF5B3FC4.toInt())

        fun fromSeed(seedArgb: Int): RecapPalette {
            val seed = Hct.fromInt(seedArgb)
            val hue = seed.hue
            val chroma = seed.chroma.coerceIn(MIN_CHROMA, MAX_CHROMA)
            fun tone(hueShift: Double, chroma: Double, tone: Double) =
                Color(Hct.from((hue + hueShift + 360.0) % 360.0, chroma, tone).toInt())
            return RecapPalette(
                background = tone(0.0, chroma * 0.8, 12.0),
                backgroundEnd = tone(18.0, chroma, 26.0),
                ink = tone(0.0, 8.0, 96.0),
                accent = tone(8.0, maxOf(chroma, ACCENT_CHROMA), 80.0),
                muted = tone(0.0, 14.0, 78.0),
                faint = tone(0.0, chroma * 0.6, 32.0),
            )
        }

        private const val MIN_CHROMA = 18.0
        private const val MAX_CHROMA = 52.0
        private const val ACCENT_CHROMA = 48.0
    }
}
