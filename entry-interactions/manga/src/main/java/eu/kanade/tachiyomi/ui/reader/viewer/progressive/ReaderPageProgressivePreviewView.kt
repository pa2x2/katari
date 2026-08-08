package eu.kanade.tachiyomi.ui.reader.viewer.progressive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.widget.ImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.chrisbanes.photoview.PhotoView
import mihon.core.common.image.progressive.ProgressiveAnimationBuffer
import kotlin.math.roundToInt

internal class ReaderPageProgressivePreviewView(
    context: Context,
    zoomEnabled: Boolean,
) : PhotoView(context) {
    private val previewDrawable = ReaderPageProgressivePreviewDrawable()
    private var bitmap: Bitmap? = null
    private var config = ReaderPageProgressivePreviewConfig()
    private var animation: ProgressiveAnimationBuffer? = null
    private var animationPosition = -1
    private var completedPlays = 0
    private var waitingForFrame = false
    private val advanceAnimationRunnable = Runnable(::advanceAnimation)

    init {
        setZoomable(zoomEnabled)
        setImageDrawable(previewDrawable)
    }

    val hasAnimation: Boolean
        get() = animation != null

    val hasCompleteAnimation: Boolean
        get() = animation?.isComplete == true

    val hasCompleteReplayableAnimation: Boolean
        get() = animation?.let { it.isComplete && it.isReplayable } == true

    fun show(bitmap: Bitmap, config: ReaderPageProgressivePreviewConfig) {
        stopAnimation()
        showBitmap(bitmap, config)
    }

    fun show(animation: ProgressiveAnimationBuffer, config: ReaderPageProgressivePreviewConfig) {
        if (animation.frames.isEmpty()) return
        val currentGeneration = this.animation
            ?.frames
            ?.getOrNull(animationPosition)
            ?.generation
        this.animation = animation
        this.config = config

        val retainedPosition = animation.frames.indexOfFirst { it.generation == currentGeneration }
        if (retainedPosition >= 0) {
            animationPosition = retainedPosition
            if (waitingForFrame && canAdvance(animation)) {
                waitingForFrame = false
                removeCallbacks(advanceAnimationRunnable)
                post(advanceAnimationRunnable)
            }
            return
        }

        completedPlays = 0
        animationPosition = 0
        waitingForFrame = false
        showCurrentAnimationFrame()
    }

    private fun showBitmap(bitmap: Bitmap, config: ReaderPageProgressivePreviewConfig) {
        val firstBitmap = this.bitmap == null
        val scaleModeChanged = this.config.scaleMode != config.scaleMode
        this.bitmap = bitmap
        this.config = config
        val layoutChanged = previewDrawable.show(bitmap, config)
        if (firstBitmap || scaleModeChanged) {
            scaleType = when (config.scaleMode) {
                ReaderPageProgressiveScaleMode.FIT_INSIDE -> ImageView.ScaleType.FIT_CENTER
                ReaderPageProgressiveScaleMode.FIT_WIDTH,
                ReaderPageProgressiveScaleMode.FIT_HEIGHT,
                ReaderPageProgressiveScaleMode.CROP,
                -> ImageView.ScaleType.CENTER_CROP
            }
        }
        if (layoutChanged) requestLayout()
        invalidate()
    }

    fun clear() {
        stopAnimation()
        if (bitmap == null) return
        bitmap = null
        previewDrawable.clear()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(advanceAnimationRunnable)
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animation != null && !waitingForFrame) {
            scheduleCurrentFrame()
        }
    }

    private fun advanceAnimation() {
        val animation = animation ?: return
        when {
            animationPosition < animation.frames.lastIndex -> {
                animationPosition++
                showCurrentAnimationFrame()
            }
            !animation.isComplete || !animation.isReplayable -> {
                waitingForFrame = true
            }
            animation.loopCount == 0 || completedPlays + 1 < (animation.loopCount ?: 1) -> {
                completedPlays++
                animationPosition = 0
                showCurrentAnimationFrame()
            }
            else -> {
                completedPlays++
                waitingForFrame = true
            }
        }
    }

    private fun showCurrentAnimationFrame() {
        val frame = animation?.frames?.getOrNull(animationPosition) ?: return
        showBitmap(frame.bitmap, config)
        scheduleCurrentFrame()
    }

    private fun scheduleCurrentFrame() {
        removeCallbacks(advanceAnimationRunnable)
        if (!isAttachedToWindow) return
        val duration = animation
            ?.frames
            ?.getOrNull(animationPosition)
            ?.frame
            ?.durationMillis
            ?.coerceAtLeast(MINIMUM_ANIMATION_FRAME_DURATION_MILLIS)
            ?: return
        postDelayed(advanceAnimationRunnable, duration)
    }

    private fun canAdvance(animation: ProgressiveAnimationBuffer): Boolean {
        return animationPosition < animation.frames.lastIndex ||
            (
                animation.isComplete && animation.isReplayable &&
                    (animation.loopCount == 0 || completedPlays + 1 < (animation.loopCount ?: 1))
                )
    }

    private fun stopAnimation() {
        removeCallbacks(advanceAnimationRunnable)
        animation = null
        animationPosition = -1
        completedPlays = 0
        waitingForFrame = false
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

private const val MINIMUM_ANIMATION_FRAME_DURATION_MILLIS = 10L
