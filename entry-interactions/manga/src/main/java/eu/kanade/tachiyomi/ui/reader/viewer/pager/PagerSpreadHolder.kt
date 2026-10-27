package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderViewerItem
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import mihon.presentation.kit.layout.pageSpreadSplit
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Two facing pages in one slot of the pager. Each is a [PagerPageHolder] of its own, so it loads, fails, zooms and
 * carries its text as a single page does. They split where [pageSpreadSplit] puts the split, on a vertical fold or in
 * half, so the page numbers Compose draws under them line up. Each page meets the other at the split: while the scale
 * type fits whole pages, a page is narrowed to its image once that is shown, instead of sitting centred in its half.
 */
@SuppressLint("ViewConstructor")
internal class PagerSpreadHolder(
    context: Context,
    private val viewer: PagerViewer,
    override val item: ReaderViewerItem.Page,
) : FrameLayout(context), ViewPagerAdapter.PositionableView {

    private val earlier = PagerPageHolder(context, viewer, item.page)
    private val later = PagerPageHolder(context, viewer, requireNotNull(item.facing))

    /** Left to right as shown: the earlier page is on the right when reading right to left. */
    val pageHolders: List<PagerPageHolder> =
        if (viewer is R2LPagerViewer) listOf(later, earlier) else listOf(earlier, later)

    init {
        pageHolders.forEach { holder ->
            holder.onImageLoaded = { requestLayout() }
            addView(holder)
        }
    }

    /** The page shown at [x] in this view's coordinates, the nearer one in the gap between them. */
    fun pageAt(x: Float): ReaderPage {
        val (left, right) = pageHolders
        return if (x < (left.right + right.left) / 2f) left.page else right.page
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        val split = split(width)
        val (left, right) = pageHolders
        left.measure(exactly(pageWidth(left, split.leftWidth, height)), exactly(height))
        right.measure(exactly(pageWidth(right, split.rightWidth, height)), exactly(height))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val split = split(r - l)
        val (left, right) = pageHolders
        left.layout(split.leftWidth - left.measuredWidth, 0, split.leftWidth, b - t)
        right.layout(split.rightStart, 0, split.rightStart + right.measuredWidth, b - t)
        viewer.onSpreadLaidOut(this)
    }

    /** The middle of the left and of the right page, as fractions of this view's width. */
    fun pageMiddles(): Pair<Float, Float>? {
        if (width == 0) return null
        val (left, right) = pageHolders
        return (left.left + left.right) / 2f / width to (right.left + right.right) / 2f / width
    }

    private fun pageWidth(holder: PagerPageHolder, sideWidth: Int, height: Int): Int {
        if (viewer.config.imageScaleType != SCALE_TYPE_CENTER_INSIDE) return sideWidth
        val aspectRatio = holder.shownAspectRatio() ?: return sideWidth
        return ceil(aspectRatio * height).toInt().coerceAtMost(sideWidth)
    }

    /** The split in pixels. The pager's own position counts, not this view's, which moves while pages are swiped. */
    private fun split(width: Int): PixelSplit {
        val density = resources.displayMetrics.density
        val pagerLeft = IntArray(2).also(viewer.pager::getLocationInWindow)[0]
        val split = pageSpreadSplit(
            width = (width / density).dp,
            left = (pagerLeft / density).dp,
            fold = viewer.activity.windowLayout?.fold,
        )
        fun Dp.toPx() = (value * density).roundToInt()
        val leftWidth = split.leftWidth.toPx().coerceIn(0, width)
        return PixelSplit(
            leftWidth = leftWidth,
            rightStart = split.rightStart.toPx().coerceIn(leftWidth, width),
            rightWidth = (width - split.rightStart.toPx()).coerceIn(0, width),
        )
    }

    private data class PixelSplit(val leftWidth: Int, val rightStart: Int, val rightWidth: Int)

    private fun exactly(size: Int) = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
}
