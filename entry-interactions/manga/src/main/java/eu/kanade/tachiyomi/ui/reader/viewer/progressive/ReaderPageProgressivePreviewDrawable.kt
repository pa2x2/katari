package eu.kanade.tachiyomi.ui.reader.viewer.progressive

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.roundToInt

internal class ReaderPageProgressivePreviewDrawable : Drawable() {
    private val bitmapPaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG,
    )
    private var bitmap: Bitmap? = null
    private var config = ReaderPageProgressivePreviewConfig()

    fun show(bitmap: Bitmap, config: ReaderPageProgressivePreviewConfig): Boolean {
        val layoutChanged = this.bitmap?.width != bitmap.width ||
            this.bitmap?.height != bitmap.height ||
            this.config != config
        this.bitmap = bitmap
        this.config = config
        invalidateSelf()
        return layoutChanged
    }

    fun clear() {
        bitmap = null
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val preview = bitmap ?: return
        val destination = RectF(bounds)
        when (val transformation = config.transformation.effective(preview)) {
            ReaderPageProgressiveTransformation.None -> {
                canvas.drawBitmap(preview, null, destination, bitmapPaint)
            }
            is ReaderPageProgressiveTransformation.CropHalf -> {
                canvas.drawBitmap(preview, transformation.side.sourceRect(preview), destination, bitmapPaint)
            }
            is ReaderPageProgressiveTransformation.Rotate -> {
                canvas.save()
                canvas.rotate(transformation.degrees, destination.centerX(), destination.centerY())
                val unrotatedDestination = RectF(
                    destination.centerX() - destination.height() / 2f,
                    destination.centerY() - destination.width() / 2f,
                    destination.centerX() + destination.height() / 2f,
                    destination.centerY() + destination.width() / 2f,
                )
                canvas.drawBitmap(preview, null, unrotatedDestination, bitmapPaint)
                canvas.restore()
            }
            is ReaderPageProgressiveTransformation.SplitAndStack -> {
                val halfHeight = destination.height() / 2f
                val upperDestination = RectF(
                    destination.left,
                    destination.top,
                    destination.right,
                    destination.top + halfHeight,
                )
                val lowerDestination = RectF(
                    destination.left,
                    destination.top + halfHeight,
                    destination.right,
                    destination.bottom,
                )
                canvas.drawBitmap(
                    preview,
                    transformation.upperSide.sourceRect(preview),
                    upperDestination,
                    bitmapPaint,
                )
                canvas.drawBitmap(
                    preview,
                    transformation.upperSide.opposite().sourceRect(preview),
                    lowerDestination,
                    bitmapPaint,
                )
            }
        }
    }

    override fun getIntrinsicWidth(): Int {
        return bitmap?.let(config.transformation::outputDimensions)?.width?.roundToInt() ?: -1
    }

    override fun getIntrinsicHeight(): Int {
        return bitmap?.let(config.transformation::outputDimensions)?.height?.roundToInt() ?: -1
    }

    override fun setAlpha(alpha: Int) {
        bitmapPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bitmapPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
