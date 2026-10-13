package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import coil3.Image

/**
 * A long strip page cut into immutable strips of a few MB each. The renderer uploads an immutable bitmap only when
 * it is drawn, so only the strips on screen reach the GPU instead of the whole page as one texture.
 */
internal class WebtoonStrips(val strips: List<Bitmap>) : Image {
    override val size: Long = strips.sumOf { it.allocationByteCount.toLong() }
    override val width: Int = strips.firstOrNull()?.width ?: 0
    override val height: Int = strips.sumOf { it.height }
    override val shareable: Boolean get() = false
    override fun draw(canvas: Canvas) {}
}
