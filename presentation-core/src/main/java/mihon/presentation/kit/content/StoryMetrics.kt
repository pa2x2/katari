package mihon.presentation.kit.content

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Fixed geometry of a shared story page, such as a statistics recap page. A page is laid out at [pageWidth] by
 * [pageHeight] and captured at three pixels per dp, so these values are positions on the shared 1080 by 1920 image
 * rather than spacing that adapts to the screen.
 */
object StoryMetrics {
    val pageWidth: Dp = 360.dp
    val pageHeight: Dp = 640.dp
    val pageSideMargin: Dp = 28.dp
    val pageTopMargin: Dp = 40.dp
    val pageBottomMargin: Dp = 22.dp

    /** How far a page's parts rise as they enter. */
    val revealRise: Dp = 28.dp

    /** How far a floating cover lifts at the top of its drift. */
    val floatLift: Dp = 7.dp

    /** Cover of the one title a page is about. */
    val coverHero: Dp = 176.dp

    /** Cover above a title's name and figures. */
    val coverFeature: Dp = 156.dp

    /** Cover beside a paragraph about it. */
    val coverStandout: Dp = 112.dp

    /** Covers of the drifting wall behind the opening page. */
    val coverWall: Dp = 84.dp

    /** Cover beside a sentence. */
    val coverMedium: Dp = 72.dp

    /** Cover in a grid of months. */
    val coverMonth: Dp = 64.dp

    /** Cover in a ranked row. */
    val coverRow: Dp = 46.dp

    /** Cover in a grid of finished titles. */
    val coverGrid: Dp = 44.dp

    /** Cover in a row of the summary card. */
    val coverSmall: Dp = 30.dp

    /** Corners of a cover of [width], in proportion so small and large covers look alike. */
    fun coverShape(width: Dp): Shape = RoundedCornerShape(width / COVER_CORNER_DIVISOR)

    /** Shadow a cover of [width] casts on the page. */
    fun coverShadow(width: Dp): Dp = (width / COVER_SHADOW_DIVISOR).coerceAtMost(coverShadowMax)

    private val coverShadowMax: Dp = 12.dp

    /** Width of the rank before a ranked row. */
    val rankWidth: Dp = 30.dp

    /** Width of the rank before a row of the summary card. */
    val rankWidthSmall: Dp = 18.dp

    /** Height of the bars comparing the months of a period. */
    val monthBarsHeight: Dp = 240.dp

    /** Size of the clock face showing the hours of a day. */
    val hourClockSize: Dp = 260.dp

    /** Room around the clock face for its hour labels. */
    val hourClockLabelSpace: Dp = 22.dp

    /** Largest a ring of the day rings grows. */
    val dayRingMax: Dp = 76.dp

    /** Smallest a ring of the day rings shrinks to fit many days. */
    val dayRingMin: Dp = 10.dp

    /** Size step the day rings shrink by while finding their size. */
    val dayRingStep: Dp = 2.dp

    /** Tallest the day rings grow. */
    val dayRingsMaxHeight: Dp = 190.dp

    /** Smallest ring that still has room for its hours inside it. */
    val dayRingLabelled: Dp = 56.dp

    /** Thinnest a ring's stroke gets. */
    val dayRingMinStroke: Dp = 2.dp

    /** Height of a bar split into shares. */
    val splitBarHeight: Dp = 22.dp

    /** Height of a bar under a ranked name. */
    val rankBarHeight: Dp = 6.dp

    /** Height of a week in a calendar of weeks. */
    val calendarWeekHeight: Dp = 34.dp

    /** Height of a week in a month calendar with day numbers. */
    val calendarMonthWeekHeight: Dp = 20.dp

    /** Height of a week in a month calendar of dots. */
    val calendarDotWeekHeight: Dp = 12.dp

    /** Gap at each end of a run's pill, so neighbouring weeks' pills read apart. */
    val calendarPillInset: Dp = 1.5.dp

    /** Distance of an active day's dot from the foot of its numbered week. */
    val calendarDotInset: Dp = 3.dp

    /** Radius of an active day's dot under its number. */
    val calendarDotRadius: Dp = 2.dp

    /** Radius of an active day's dot standing in for its number. */
    val calendarActiveDotRadius: Dp = 2.5.dp

    /** Radius of a day's dot in a calendar of dots. */
    val calendarDayDotRadius: Dp = 1.5.dp

    /** Width of the drifting cover wall, wider than the page so its tilted rows reach past both edges. */
    val coverWallWidth: Dp = 470.dp

    /** Height of the progress bars above a story. */
    val progressHeight: Dp = 3.dp

    /** Height of the controls below a story. */
    val controlsHeight: Dp = 64.dp

    /** How far a story is dragged down to close it. */
    val dismissDrag: Dp = 120.dp
}

private const val COVER_CORNER_DIVISOR = 18
private const val COVER_SHADOW_DIVISOR = 14
