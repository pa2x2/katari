package mihon.presentation.kit.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colours statistics charts draw with, read through KatariTheme.chart: series accents, the tracks and cells they fill,
 * and rules, hatching and marks laid translucently over the plot so the data stays in front.
 */
@Immutable
class ChartColors internal constructor(scheme: ColorScheme, val accents: List<Color>) {
    /** The series when no accent applies, such as every type together, and the selected point of a tracked period. */
    val series: Color = scheme.primary

    /** Empty part of a bar and a calendar day without activity; intensity ramps start from it. */
    val track: Color = scheme.surfaceVariant

    /** Marks that belong to no series: labels drawn on the chart and the selected point of an untracked period. */
    val neutral: Color = scheme.onSurfaceVariant

    /** Ring around the selected point, cut out of what lies under it. */
    val halo: Color = scheme.surface

    /** Swatch and hatching of time known only as a total, outside the plot. */
    val untracked: Color = scheme.outline

    /** Outline of a legend sample and hatching over calendar days. */
    val outline: Color = scheme.outlineVariant

    /** Value rules across the plot and its side borders. */
    val gridLine: Color = outline.copy(alpha = GRID_LINE_ALPHA)

    /** Rules marking the labelled points along the time axis, fainter than [gridLine]. */
    val tickLine: Color = outline.copy(alpha = TICK_LINE_ALPHA)

    /** Diagonal hatching over periods that have no records, inside the plot. */
    val hatch: Color = outline.copy(alpha = HATCH_ALPHA)

    /** Line from the selected point down the plot. */
    val selectionGuide: Color = neutral.copy(alpha = SELECTION_GUIDE_ALPHA)

    /** Fill of a share that has no detailed records, such as time known only as a total. */
    val untrackedFill: Color = untracked.copy(alpha = UNTRACKED_FILL_ALPHA)

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
