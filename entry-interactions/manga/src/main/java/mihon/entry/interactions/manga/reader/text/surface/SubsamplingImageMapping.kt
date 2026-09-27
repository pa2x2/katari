package mihon.entry.interactions.manga.reader.text.surface

import android.graphics.RectF
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize

/*
 * Mapping between recognized-image pixels and a subsampling view. The view's source may be a scaled bitmap of the
 * same displayed image (the webtoon viewer decodes long strips at view width), so image pixels are first scaled to
 * the view's source size.
 */

/** Writes where [rect] of an image of [imageSize] appears in this view's coordinates into [target]. */
internal fun SubsamplingScaleImageView.imageToView(rect: ImageRect, imageSize: ImageSize, target: RectF): Boolean {
    if (!isReady || sWidth == 0 || sHeight == 0) return false
    val scaleX = sWidth.toFloat() / imageSize.width
    val scaleY = sHeight.toFloat() / imageSize.height
    val topLeft = sourceToViewCoord(rect.left * scaleX, rect.top * scaleY) ?: return false
    val bottomRight = sourceToViewCoord(rect.right * scaleX, rect.bottom * scaleY) ?: return false
    target.set(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    return true
}

/**
 * The image coordinates of view point ([x], [y]). Points beside the image map outside the image bounds, so callers
 * can clip outlines that extend past the page.
 */
internal fun SubsamplingScaleImageView.viewToImage(x: Float, y: Float, imageSize: ImageSize): Pair<Int, Int>? {
    if (!isReady || sWidth == 0 || sHeight == 0) return null
    val source = viewToSourceCoord(x, y) ?: return null
    return (source.x * imageSize.width / sWidth).toInt() to (source.y * imageSize.height / sHeight).toInt()
}
