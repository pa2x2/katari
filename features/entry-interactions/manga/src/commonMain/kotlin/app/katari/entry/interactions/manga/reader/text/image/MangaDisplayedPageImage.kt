package app.katari.entry.interactions.manga.reader.text.image

import androidx.compose.ui.unit.IntRect
import app.katari.core.image.decode.ImageRegionDecoder
import app.katari.entry.interactions.manga.reader.text.geometry.MangaDisplayedPageGeometry
import app.katari.text.recognition.api.image.ImageContentKey
import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.image.ImageSize
import app.katari.text.recognition.api.image.TextRecognitionBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString

/**
 * The still image a reader page currently displays, decoded exactly as the viewer decodes it.
 *
 * [encoded] is the page file and [cropBorders] the viewer's border-cropping flag. Decoding with the same decoder and
 * flag places recognized regions in the coordinates of the image the viewer draws its pieces from.
 */
internal class MangaDisplayedPageImage private constructor(
    private val encoded: ByteString,
    private val cropBorders: Boolean,
    override val key: ImageContentKey,
    override val size: ImageSize,
    override val geometry: MangaDisplayedPageGeometry,
    override val rawContent: ImageContentKey,
    private var decoder: ImageRegionDecoder?,
) : DisplayedPageImage {

    override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): TextRecognitionBitmap =
        withContext(Dispatchers.IO) {
            synchronized(this@MangaDisplayedPageImage) {
                val decoder = decoder ?: newDecoder(encoded, cropBorders).also { decoder = it }
                val decoded = decoder.decode(IntRect(region.left, region.top, region.right, region.bottom), sampleSize)
                    ?: error("Page region $region could not be decoded")
                decoded.toTextRecognitionBitmap()
            }
        }

    @Synchronized
    override fun close() {
        decoder?.close()
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
                        shown = decoder.bounds.let { ImageRect(it.left, it.top, it.right, it.bottom) },
                    ),
                    rawContent = still.rawContent ?: ImageContentKey(content),
                    decoder = decoder,
                )
            } catch (error: Throwable) {
                decoder.close()
                throw error
            }
        }

        private fun newDecoder(encoded: ByteString, cropBorders: Boolean): ImageRegionDecoder =
            requireNotNull(ImageRegionDecoder.open(encoded.toByteArray().inputStream(), cropBorders)) {
                "Unsupported page image"
            }

        private const val CROPPED_SUFFIX = ":cropped"
    }
}
