package mihon.entry.interactions.manga.reader.text.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextDecoration
import mihon.entry.interactions.manga.reader.text.surface.imageToView

/** Outlines recognized text regions on a page, in the page view's own drawing pass. */
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
    private val rect = RectF()

    fun draw(view: SubsamplingScaleImageView, canvas: Canvas, decoration: MangaPageTextDecoration) {
        val radius = CORNER_RADIUS_DP * density
        decoration.regions.forEach { region ->
            if (!view.imageToView(region, decoration.imageSize, rect)) return@forEach
            canvas.drawRoundRect(rect, radius, radius, fill)
            canvas.drawRoundRect(rect, radius, radius, outline)
        }
        // The highlighted text may be a region or an area the user outlined.
        val highlighted = decoration.highlighted ?: return
        if (view.imageToView(highlighted, decoration.imageSize, rect)) {
            canvas.drawRoundRect(rect, radius, radius, highlight)
            canvas.drawRoundRect(rect, radius, radius, outline)
        }
    }

    private companion object {
        const val OUTLINE_WIDTH_DP = 1.5f
        const val CORNER_RADIUS_DP = 6f
    }
}
