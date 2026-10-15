package mihon.presentation.kit.content

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fixed geometry of statistics charts and the rows that show figures beside them, which the spacing scale has no step
 * for. Charts draw and hit-test with the same values, so both read them here.
 */
object StatisticsMetrics {
    /** Height of the activity chart's plot, and of the space it holds while it first loads. */
    val chartHeight: Dp = 180.dp

    /** Width of the duration labels beside the plot. */
    val chartAxisWidth: Dp = 48.dp

    /** Space between the plot's side borders and the edge of the chart. */
    val chartSideInset: Dp = 12.dp

    /** Space between the plot's side borders and the first and last bar centres; keeps the widest bars clear of them. */
    val chartEdgePointInset: Dp = 16.dp

    /** Space above and below the plot inside the chart. */
    val chartVerticalInset: Dp = 6.dp

    val barMinWidth: Dp = 1.dp
    val barMaxWidth: Dp = 24.dp

    /** Radius of the ring around the selected point, in the surface colour. */
    val selectionHaloRadius: Dp = 6.dp

    /** Radius of the dot on the selected point. */
    val selectionDotRadius: Dp = 4.dp

    /** Distance between the diagonal lines hatching untracked periods. */
    val hatchSpacing: Dp = 6.dp

    /** Size of the hatched square in the legend that explains it. */
    val hatchSample: Dp = 12.dp

    /** Height of the request status laid over the top of the chart while another period loads. */
    val chartStatusHeight: Dp = 40.dp

    /** Height of the activity card's message when the All range has no activity. */
    val emptyActivityHeight: Dp = 140.dp

    /** Height of hourly rhythm bars. */
    val rhythmHeight: Dp = 64.dp

    /** Height of weekday and monthly rhythm bars. */
    val rhythmCompactHeight: Dp = 48.dp

    /** Room under rhythm bars for their labels. */
    val rhythmLabelArea: Dp = 20.dp

    val rhythmBarCorner: Dp = 2.dp

    /** The least a rhythm bar with any value shows. */
    val rhythmBarMinHeight: Dp = 2.dp

    /** One day of the reading calendar. */
    val calendarCell: Dp = 14.dp
    val calendarCellGap: Dp = 3.dp
    val calendarLegendCell: Dp = 10.dp

    /** Room above the calendar's weeks for month names. */
    val calendarMonthLabelHeight: Dp = 18.dp

    /** Bar showing a title's or label's share. */
    val shareBar: Dp = 4.dp

    /** Bar comparing whole totals, such as time by entry type. */
    val shareBarThick: Dp = 8.dp

    /** Bar split into progress stages or tracked and earlier time. */
    val stageBar: Dp = 12.dp

    /** Stage bar under a row of a list. */
    val stageBarCompact: Dp = 6.dp

    /** Width of a cover in a ranked title row. */
    val rankedCoverWidth: Dp = 40.dp

    /** Width of the rank before a ranked title row. */
    val rankWidth: Dp = 24.dp

    /** Width of the time and share after a ranked title row, so every share bar has the same scale. */
    val rankedTrailingWidth: Dp = 64.dp

    /** Width of a share percentage in a legend, so the counts before it line up. */
    val percentWidth: Dp = 40.dp
}
