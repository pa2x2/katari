package mihon.entry.interactions.manga.reader.text.image

import android.graphics.Bitmap
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.image.TextRecognitionImage
import okio.ByteString
import tachiyomi.decoder.ImageDecoder

/** A displayed page image that holds decoding resources until it is closed. */
internal interface DisplayedPageImage : TextRecognitionImage, AutoCloseable

/**
 * The still image a reader page currently displays, decoded exactly as the viewer decodes it.
 *
 * [encoded] holds the bytes after the viewer's own processing (dual-page split or rotation), and [cropBorders] is the
 * viewer's border-cropping flag. Decoding with the same decoder and flag places recognized regions in the
 * coordinates the viewer draws, so no separate transform between recognition and display exists.
 */
internal class MangaDisplayedPageImage private constructor(
    private val encoded: ByteString,
    private val cropBorders: Boolean,
    override val key: ImageContentKey,
    override val size: ImageSize,
) : DisplayedPageImage {
    private var decoder: ImageDecoder? = null

    override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): Bitmap = withContext(Dispatchers.IO) {
        synchronized(this@MangaDisplayedPageImage) {
            val decoder = decoder ?: newDecoder(encoded, cropBorders).also { decoder = it }
            decoder.decode(Rect(region.left, region.top, region.right, region.bottom), sampleSize)
                ?: error("Page region $region could not be decoded")
        }
    }

    @Synchronized
    override fun close() {
        decoder?.recycle()
        decoder = null
    }

    companion object {
        /** Reads the displayed dimensions; returns `null` for content the decoder cannot read. */
        suspend fun open(encoded: ByteString, cropBorders: Boolean): MangaDisplayedPageImage? =
            withContext(Dispatchers.IO) {
                val decoder = runCatching { newDecoder(encoded, cropBorders) }.getOrNull() ?: return@withContext null
                try {
                    MangaDisplayedPageImage(
                        encoded = encoded,
                        cropBorders = cropBorders,
                        key = ImageContentKey(encoded.sha256().hex() + if (cropBorders) CROPPED_SUFFIX else ""),
                        size = ImageSize(decoder.width, decoder.height),
                    )
                } finally {
                    decoder.recycle()
                }
            }

        private fun newDecoder(encoded: ByteString, cropBorders: Boolean): ImageDecoder =
            requireNotNull(ImageDecoder.newInstance(encoded.toByteArray().inputStream(), cropBorders)) {
                "Unsupported page image"
            }

        private const val CROPPED_SUFFIX = ":cropped"
    }
}
