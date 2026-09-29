package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

/**
 * Subsampling view that draws page decorations in the same pass as the page, so they follow panning, zooming, and
 * animations frame by frame.
 */
internal open class ReaderSubsamplingImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SubsamplingScaleImageView(context, attrs) {

    var foregroundPainter: ((SubsamplingScaleImageView, Canvas) -> Unit)? = null
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isReady) foregroundPainter?.invoke(this, canvas)
    }
}
