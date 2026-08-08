package eu.kanade.tachiyomi.ui.reader.viewer.progressive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlin.math.roundToInt

internal class ReaderPageProgressivePreviewView(
    context: Context,
) : View(context) {
    private val bitmapPaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG,
    )
    private var bitmap: Bitmap? = null
    private var config = ReaderPageProgressivePreviewConfig()

    fun show(bitmap: Bitmap, config: ReaderPageProgressivePreviewConfig) {
        val layoutChanged = this.bitmap?.width != bitmap.width ||
            this.bitmap?.height != bitmap.height ||
            this.config != config
        this.bitmap = bitmap
        this.config = config
        if (layoutChanged) requestLayout()
        invalidate()
    }

    fun clear() {
        if (bitmap == null) return
        bitmap = null
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val preview = bitmap
        if (preview == null || config.scaleMode != ReaderPageProgressiveScaleMode.FIT_WIDTH) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val dimensions = config.transformation.outputDimensions(preview)
        val desiredWidth = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.UNSPECIFIED -> dimensions.width.roundToInt()
            else -> MeasureSpec.getSize(widthMeasureSpec)
        }
        val desiredHeight = (desiredWidth * dimensions.height / dimensions.width).roundToInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val preview = bitmap ?: return
        val dimensions = config.transformation.outputDimensions(preview)
        val destination = destinationRect(dimensions)

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

    private fun destinationRect(dimensions: PreviewDimensions): RectF {
        val scale = when (config.scaleMode) {
            ReaderPageProgressiveScaleMode.FIT_INSIDE -> minOf(
                width / dimensions.width,
                height / dimensions.height,
            )
            ReaderPageProgressiveScaleMode.FIT_WIDTH -> width / dimensions.width
            ReaderPageProgressiveScaleMode.FIT_HEIGHT -> height / dimensions.height
            ReaderPageProgressiveScaleMode.CROP -> maxOf(
                width / dimensions.width,
                height / dimensions.height,
            )
        }
        val outputWidth = dimensions.width * scale
        val outputHeight = dimensions.height * scale
        val left = (width - outputWidth) / 2f
        val top = (height - outputHeight) / 2f
        return RectF(left, top, left + outputWidth, top + outputHeight)
    }
}

internal data class ReaderPageProgressivePreviewConfig(
    val scaleMode: ReaderPageProgressiveScaleMode = ReaderPageProgressiveScaleMode.FIT_INSIDE,
    val transformation: ReaderPageProgressiveTransformation = ReaderPageProgressiveTransformation.None,
)

internal enum class ReaderPageProgressiveScaleMode {
    FIT_INSIDE,
    FIT_WIDTH,
    FIT_HEIGHT,
    CROP,
}

internal fun Int.toReaderPageProgressiveScaleMode(): ReaderPageProgressiveScaleMode = when (this) {
    SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP -> ReaderPageProgressiveScaleMode.CROP
    SubsamplingScaleImageView.SCALE_TYPE_FIT_WIDTH -> ReaderPageProgressiveScaleMode.FIT_WIDTH
    SubsamplingScaleImageView.SCALE_TYPE_FIT_HEIGHT -> ReaderPageProgressiveScaleMode.FIT_HEIGHT
    else -> ReaderPageProgressiveScaleMode.FIT_INSIDE
}

internal sealed interface ReaderPageProgressiveTransformation {
    data object None : ReaderPageProgressiveTransformation

    data class Rotate(
        val degrees: Float,
    ) : ReaderPageProgressiveTransformation

    data class CropHalf(
        val side: ReaderPageProgressiveSide,
    ) : ReaderPageProgressiveTransformation

    data class SplitAndStack(
        val upperSide: ReaderPageProgressiveSide,
    ) : ReaderPageProgressiveTransformation

    fun effective(bitmap: Bitmap): ReaderPageProgressiveTransformation {
        return if (bitmap.width > bitmap.height) this else None
    }

    fun outputDimensions(bitmap: Bitmap): PreviewDimensions {
        return when (effective(bitmap)) {
            None -> PreviewDimensions(bitmap.width.toFloat(), bitmap.height.toFloat())
            is Rotate -> PreviewDimensions(bitmap.height.toFloat(), bitmap.width.toFloat())
            is CropHalf -> PreviewDimensions((bitmap.width / 2).toFloat(), bitmap.height.toFloat())
            is SplitAndStack -> PreviewDimensions((bitmap.width / 2).toFloat(), bitmap.height * 2f)
        }
    }
}

internal enum class ReaderPageProgressiveSide {
    LEFT,
    RIGHT,
    ;

    fun opposite(): ReaderPageProgressiveSide = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
    }

    fun sourceRect(bitmap: Bitmap): Rect {
        val halfWidth = bitmap.width / 2
        return when (this) {
            LEFT -> Rect(0, 0, halfWidth, bitmap.height)
            RIGHT -> Rect(bitmap.width - halfWidth, 0, bitmap.width, bitmap.height)
        }
    }
}

internal data class PreviewDimensions(
    val width: Float,
    val height: Float,
)
