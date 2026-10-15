package mihon.presentation.kit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colours statistics charts draw besides their series accents: rules, hatching and marks laid translucently over the
 * plot so the data stays in front. Series themselves take [KatariColors.chartAccents].
 */
@Immutable
class ChartColors internal constructor(outline: Color, outlineVariant: Color, onSurfaceVariant: Color) {
    /** Value rules across the plot and its side borders. */
    val gridLine: Color = outlineVariant.copy(alpha = GRID_LINE_ALPHA)

    /** Rules marking the labelled points along the time axis, fainter than [gridLine]. */
    val tickLine: Color = outlineVariant.copy(alpha = TICK_LINE_ALPHA)

    /** Diagonal hatching over periods that have no records, inside the plot. */
    val hatch: Color = outlineVariant.copy(alpha = HATCH_ALPHA)

    /** Line from the selected point down the plot. */
    val selectionGuide: Color = onSurfaceVariant.copy(alpha = SELECTION_GUIDE_ALPHA)

    /** Fill of a share that has no detailed records, such as time known only as a total. */
    val untrackedFill: Color = outline.copy(alpha = UNTRACKED_FILL_ALPHA)

    /** A bar of the series in [accent]; slightly see-through so grid lines read through stacked bars. */
    fun seriesFill(accent: Color): Color = accent.copy(alpha = SERIES_FILL_ALPHA)

    /** A bar of the series in [accent] that steps back so a highlighted bar beside it stands out. */
    fun seriesMuted(accent: Color): Color = accent.copy(alpha = SERIES_MUTED_ALPHA)
}

private const val GRID_LINE_ALPHA = 0.45f
private const val TICK_LINE_ALPHA = 0.2f
private const val HATCH_ALPHA = 0.55f
private const val SELECTION_GUIDE_ALPHA = 0.55f
private const val UNTRACKED_FILL_ALPHA = 0.35f
private const val SERIES_FILL_ALPHA = 0.78f
private const val SERIES_MUTED_ALPHA = 0.5f
