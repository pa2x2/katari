package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct

/**
 * The colours of a shared story page, such as a statistics recap page, built from one seed colour: a cover's most
 * characteristic colour, or the theme's primary. Every role sits at a fixed tone, so text stays readable on the
 * background whatever the seed; only hue and colourfulness follow it. [StoryTheme] derives the kit colour roles from
 * it; drawing on the page reads the properties here.
 */
@Immutable
data class StoryPalette(
    val background: Color,
    /** Where the page's vertical gradient ends, at its foot. */
    val backgroundEnd: Color,
    val ink: Color,
    val accent: Color,
    val muted: Color,
    /** Faint marks on the background, such as empty calendar days and bar tracks. */
    val faint: Color,
) {
    /** Accent marks that step back so a highlighted one beside them stands out. */
    val accentMuted: Color get() = accent.copy(alpha = ACCENT_MUTED_ALPHA)

    /** Track behind progress drawn in [ink]. */
    val inkTrack: Color get() = ink.copy(alpha = INK_TRACK_ALPHA)

    /** Marks for days outside the period a calendar covers. */
    val inkFaint: Color get() = ink.copy(alpha = INK_FAINT_ALPHA)

    /** Labels for days outside the period a calendar covers. */
    val mutedFaint: Color get() = muted.copy(alpha = MUTED_FAINT_ALPHA)

    /** The light that runs along a finished bar to draw the eye to it. */
    val glint: Color get() = GLINT

    /** Colours of the parts of a split bar, in order, repeating after the last. */
    val series: List<Color> get() = listOf(
        accent,
        ink.copy(alpha = SERIES_INK_ALPHA),
        muted.copy(alpha = SERIES_MUTED_ALPHA),
    )

    /** A soft glow of [color] drifting over the backdrop, at [strength] from invisible at 0 to solid at 1. */
    fun glow(color: Color, strength: Float): Color = color.copy(alpha = strength.coerceIn(0f, 1f))

    companion object {
        /** The palette of the app theme's primary colour, for pages without a seed and what points to a story. */
        val App: StoryPalette
            @Composable
            get() {
                val primary = KatariTheme.colors.primary
                return remember(primary) { fromSeed(primary.toArgb()) }
            }

        fun fromSeed(seedArgb: Int): StoryPalette {
            val seed = Hct.fromInt(seedArgb)
            val hue = seed.hue
            val chroma = seed.chroma.coerceIn(MIN_CHROMA, MAX_CHROMA)
            fun tone(hueShift: Double, chroma: Double, tone: Double) =
                Color(Hct.from((hue + hueShift + 360.0) % 360.0, chroma, tone).toInt())
            return StoryPalette(
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

private const val ACCENT_MUTED_ALPHA = 0.45f
private const val INK_TRACK_ALPHA = 0.3f
private const val INK_FAINT_ALPHA = 0.22f
private const val MUTED_FAINT_ALPHA = 0.35f
private const val SERIES_INK_ALPHA = 0.75f
private const val SERIES_MUTED_ALPHA = 0.5f
private val GLINT = Color.White.copy(alpha = 0.7f)
