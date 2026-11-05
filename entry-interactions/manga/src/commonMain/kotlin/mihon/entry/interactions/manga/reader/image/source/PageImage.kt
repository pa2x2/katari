package mihon.entry.interactions.manga.reader.image.source

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.core.image.PlatformBitmap
import mihon.core.image.decode.ImageRegionDecoder
import mihon.core.image.decode.detectImageType

/** A reader page's image, ready to draw: [size] is what the viewer lays out, in the image's own pixels. */
internal sealed interface PageImage {
    val size: IntSize

    /**
     * A still image, drawn from regions decoded as they come into view. [size] is the image without its borders when
     * they are cropped. Holds the native decoder until [close].
     */
    class Still internal constructor(private val decoder: ImageRegionDecoder) : PageImage, AutoCloseable {
        override val size = IntSize(decoder.width, decoder.height)

        // The native decoder serves one region at a time and must not be closed during a decode
        private val lock = Any()
        private var closed = false

        /** [region] of the image divided by [sampleSize]; null once closed or when the region can't be decoded. */
        suspend fun decode(region: IntRect, sampleSize: Int): PlatformBitmap? = withContext(Dispatchers.Default) {
            synchronized(lock) { if (closed) null else decoder.decode(region, sampleSize) }
        }

        /** Waits for a decode in progress, which takes a few milliseconds, then frees the decoder. */
        override fun close() = synchronized(lock) {
            if (!closed) {
                closed = true
                decoder.close()
            }
        }
    }

    /** An animated image, played from its encoded [bytes]; borders are not cropped from animations. */
    class Animated internal constructor(val bytes: ByteArray, override val size: IntSize) : PageImage
}

/**
 * Opens a page's encoded image [bytes], or returns null for content the decoder can't read. [cropBorders] removes the
 * plain borders of a still image; [displayProfile] is the ICC profile of the screen, which pixels are converted to.
 */
internal suspend fun openPageImage(bytes: ByteArray, cropBorders: Boolean, displayProfile: ByteArray?): PageImage? =
    withContext(Dispatchers.Default) {
        val isAnimated = detectImageType(bytes.copyOf(minOf(bytes.size, TYPE_HEADER_BYTES)))?.isAnimated == true
        val decoder = ImageRegionDecoder.open(bytes.inputStream(), cropBorders && !isAnimated, displayProfile)
            ?: return@withContext null
        if (isAnimated) {
            val size = IntSize(decoder.width, decoder.height)
            decoder.close()
            PageImage.Animated(bytes, size)
        } else {
            PageImage.Still(decoder)
        }
    }

/** Enough of the file for the decoder to tell its format and whether it is animated. */
private const val TYPE_HEADER_BYTES = 64 * 1024
