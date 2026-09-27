package mihon.text.recognition.api.image

import android.graphics.Bitmap
import java.io.InputStream
import java.security.MessageDigest

/**
 * A host-owned image that recognition reads region by region.
 *
 * Hosts own decoding because only they know every format they display. Coordinates are source-image pixels.
 */
interface TextRecognitionImage {
    /** Stable identity of the encoded image content, used to reuse recognition results. */
    val key: ImageContentKey
    val size: ImageSize

    /**
     * Decodes [region] subsampled by [sampleSize] (a power of two). The returned bitmap is owned by the caller.
     */
    suspend fun decodeRegion(region: ImageRect, sampleSize: Int): Bitmap
}

@JvmInline
value class ImageContentKey(
    val value: String,
) {
    init {
        require(value.isNotBlank())
    }

    companion object {
        /** Identifies encoded image bytes by their SHA-256 digest. The stream is read to its end but not closed. */
        fun sha256(input: InputStream): ImageContentKey {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
            return ImageContentKey(digest.digest().joinToString("") { "%02x".format(it) })
        }
    }
}

data class ImageSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0 && height > 0)
    }

    val bounds: ImageRect
        get() = ImageRect(0, 0, width, height)
}

/** A half-open pixel rectangle: [left, right) × [top, bottom). */
data class ImageRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(right > left && bottom > top) { "Empty image rectangle: $this" }
    }

    val width: Int
        get() = right - left

    val height: Int
        get() = bottom - top

    val area: Long
        get() = width.toLong() * height

    fun contains(x: Int, y: Int): Boolean = x in left until right && y in top until bottom

    fun intersect(other: ImageRect): ImageRect? {
        val l = maxOf(left, other.left)
        val t = maxOf(top, other.top)
        val r = minOf(right, other.right)
        val b = minOf(bottom, other.bottom)
        return if (r > l && b > t) ImageRect(l, t, r, b) else null
    }

    fun union(other: ImageRect): ImageRect = ImageRect(
        minOf(left, other.left),
        minOf(top, other.top),
        maxOf(right, other.right),
        maxOf(bottom, other.bottom),
    )
}
