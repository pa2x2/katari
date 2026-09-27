package mihon.entry.interactions.manga.reader.text.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.ColorUtils
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import mihon.entry.interactions.manga.reader.text.session.MangaPageTextOverlay
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextDecoration
import mihon.entry.interactions.manga.reader.text.surface.imageToView
import kotlin.math.roundToInt

/**
 * Draws recognized text outlines and translations on a page, in the page view's own drawing pass.
 *
 * A translation covers the original text with its background color and is set in the largest size that fits its
 * area at the current zoom. Layouts are reused while the area keeps its size, so panning does not relayout text.
 */
internal class MangaPageTextOverlayPainter(context: Context) {
    private val density = context.resources.displayMetrics.density
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = OUTLINE_WIDTH_DP * density
        color = Color.argb(200, 64, 132, 255)
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(28, 64, 132, 255)
    }
    private val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(72, 64, 132, 255)
    }
    private val cover = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val rect = RectF()
    private val layouts = HashMap<MangaPageTextOverlay, FittedLayout>()

    fun draw(view: SubsamplingScaleImageView, canvas: Canvas, decoration: MangaPageTextDecoration) {
        val radius = CORNER_RADIUS_DP * density
        decoration.regions.forEach { region ->
            if (!view.imageToView(region, decoration.imageSize, rect)) return@forEach
            canvas.drawRoundRect(rect, radius, radius, fill)
            canvas.drawRoundRect(rect, radius, radius, outline)
        }
        layouts.keys.retainAll(decoration.overlays.toSet())
        decoration.overlays.forEach { overlay ->
            if (!view.imageToView(overlay.area, decoration.imageSize, rect)) return@forEach
            drawTranslation(canvas, overlay, rect, radius)
        }
        // The highlighted text may be a region or an area the user outlined.
        val highlighted = decoration.highlighted ?: return
        if (view.imageToView(highlighted, decoration.imageSize, rect)) {
            canvas.drawRoundRect(rect, radius, radius, highlight)
            canvas.drawRoundRect(rect, radius, radius, outline)
        }
    }

    private fun drawTranslation(canvas: Canvas, overlay: MangaPageTextOverlay, area: RectF, radius: Float) {
        cover.color = overlay.background
        canvas.drawRoundRect(area, radius, radius, cover)
        val padding = TEXT_PADDING_DP * density
        val width = (area.width() - 2 * padding).roundToInt()
        val height = (area.height() - 2 * padding).roundToInt()
        if (width <= 0 || height <= 0) return
        val layout = layouts[overlay]?.takeIf { it.width == width && it.height == height }
            ?: fit(overlay, width, height).also { layouts[overlay] = it }
        canvas.save()
        canvas.translate(area.left + padding, area.top + padding + (height - layout.layout.height) / 2f)
        layout.layout.draw(canvas)
        canvas.restore()
    }

    /** The largest text size whose layout fits [width] × [height], down to a legibility floor. */
    private fun fit(overlay: MangaPageTextOverlay, width: Int, height: Int): FittedLayout {
        textPaint.color = if (ColorUtils.calculateLuminance(overlay.background) > DARK_BACKGROUND_LUMINANCE) {
            Color.BLACK
        } else {
            Color.WHITE
        }
        val longestWord = overlay.text.split(WHITESPACE).maxByOrNull(String::length).orEmpty()
        var low = MINIMUM_TEXT_SIZE_DP * density
        var high = maxOf(low, height.toFloat())
        var best = layout(overlay.text, width, low)
        repeat(FIT_ITERATIONS) {
            val size = (low + high) / 2
            val candidate = layout(overlay.text, width, size)
            if (candidate.height <= height && textPaint.measureText(longestWord) <= width) {
                best = candidate
                low = size
            } else {
                high = size
            }
        }
        return FittedLayout(best, width, height)
    }

    private fun layout(text: String, width: Int, textSize: Float): StaticLayout {
        textPaint.textSize = textSize
        return StaticLayout.Builder.obtain(text, 0, text.length, TextPaint(textPaint), width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(0f, LINE_SPACING)
            .build()
    }

    private class FittedLayout(
        val layout: StaticLayout,
        val width: Int,
        val height: Int,
    )

    private companion object {
        const val OUTLINE_WIDTH_DP = 1.5f
        const val CORNER_RADIUS_DP = 6f
        const val TEXT_PADDING_DP = 2f
        const val MINIMUM_TEXT_SIZE_DP = 7f
        const val LINE_SPACING = 0.95f
        const val FIT_ITERATIONS = 10
        const val DARK_BACKGROUND_LUMINANCE = 0.45

        /** Words are not broken across lines unless even the smallest size cannot fit them. */
        val WHITESPACE = Regex("""\s+""")
    }
}
