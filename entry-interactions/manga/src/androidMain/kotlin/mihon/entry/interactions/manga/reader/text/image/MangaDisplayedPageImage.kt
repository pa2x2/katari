package mihon.entry.interactions.manga.reader.text.image

import android.graphics.Bitmap
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.entry.interactions.manga.reader.text.geometry.MangaDisplayedPageGeometry
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.image.TextRecognitionImage
import okio.ByteString
import tachiyomi.decoder.ImageDecoder

/** A displayed page image that holds decoding resources until it is closed. */
internal interface DisplayedPageImage : TextRecognitionImage, AutoCloseable {
    /** How this image relates to the raw page, for placing what was recognized on the raw page. */
    val geometry: MangaDisplayedPageGeometry

    /** Identity of the raw page file this image shows, as stored translations record it. */
    val rawContent: ImageContentKey
}

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
    override val geometry: MangaDisplayedPageGeometry,
    override val rawContent: ImageContentKey,
    private var decoder: ImageDecoder?,
) : DisplayedPageImage {

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
        /**
         * Reads the displayed dimensions; returns `null` for content the decoder cannot read. The decoder that read
         * them decodes regions later, because creating one decodes the whole image when borders are cropped.
         */
        suspend fun open(still: DisplayedStillImage): MangaDisplayedPageImage? = withContext(Dispatchers.IO) {
            val decoder = runCatching { newDecoder(still.encoded, still.cropBorders) }.getOrNull()
                ?: return@withContext null
            try {
                val content = still.encoded.sha256().hex()
                MangaDisplayedPageImage(
                    encoded = still.encoded,
                    cropBorders = still.cropBorders,
                    key = ImageContentKey(content + if (still.cropBorders) CROPPED_SUFFIX else ""),
                    size = ImageSize(decoder.width, decoder.height),
                    geometry = MangaDisplayedPageGeometry(
                        transform = still.transform,
                        shown = decoder.bounds.let { ImageRect(it.left, it.top, it.right, it.bottom) },
                    ),
                    rawContent = still.rawContent ?: ImageContentKey(content),
                    decoder = decoder,
                )
            } catch (error: Throwable) {
                decoder.recycle()
                throw error
            }
        }

        private fun newDecoder(encoded: ByteString, cropBorders: Boolean): ImageDecoder =
            requireNotNull(ImageDecoder.newInstance(encoded.toByteArray().inputStream(), cropBorders)) {
                "Unsupported page image"
            }

        private const val CROPPED_SUFFIX = ":cropped"
    }
}
