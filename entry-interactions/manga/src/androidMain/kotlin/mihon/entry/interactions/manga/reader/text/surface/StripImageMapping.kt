package mihon.entry.interactions.manga.reader.text.surface

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonStripImageView
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize

/*
 * Mapping between recognized-image pixels and a long strip page view, which always shows the whole page scaled to
 * its own size.
 */

/** Writes where [rect] of an image of [imageSize] appears in this view's coordinates into [target]. */
internal fun WebtoonStripImageView.imageToView(rect: ImageRect, imageSize: ImageSize, target: RectF): Boolean {
    if (!hasImage || width == 0 || height == 0) return false
    val scaleX = width.toFloat() / imageSize.width
    val scaleY = height.toFloat() / imageSize.height
    target.set(rect.left * scaleX, rect.top * scaleY, rect.right * scaleX, rect.bottom * scaleY)
    return true
}

/** The image coordinates of view point ([x], [y]), outside the image bounds for points beside it. */
internal fun WebtoonStripImageView.viewToImage(x: Float, y: Float, imageSize: ImageSize): Pair<Int, Int>? {
    if (!hasImage || width == 0 || height == 0) return null
    return (x * imageSize.width / width).toInt() to (y * imageSize.height / height).toInt()
}
