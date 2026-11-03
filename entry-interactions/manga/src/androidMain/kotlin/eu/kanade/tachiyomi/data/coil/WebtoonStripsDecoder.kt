package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.Matrix
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.decoder.ImageDecoder
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Decodes a long strip page into [WebtoonStrips]. A page drawn as one bitmap is uploaded to the GPU as one texture
 * the moment it scrolls into view, which stalls scrolling between pages.
 */
internal class WebtoonStripsDecoder(private val resources: ImageSource, private val options: Options) : Decoder {

    override suspend fun decode(): DecodeResult = stripDecodes.withPermit {
        val decoder = resources.sourceOrNull()?.use {
            ImageDecoder.newInstance(it.inputStream(), options.cropBorders, TachiyomiImageDecoder.displayProfile)
        }
        check(decoder != null && decoder.width > 0 && decoder.height > 0) { "Failed to initialize decoder" }

        val viewWidth = options.size.widthPx(options.scale) { decoder.width }
        val sampleSize = DecodeUtils.calculateInSampleSize(
            srcWidth = decoder.width,
            srcHeight = decoder.height,
            dstWidth = viewWidth,
            dstHeight = options.size.heightPx(options.scale) { decoder.height },
            scale = options.scale,
        )
        val page = try {
            decoder.decode(sampleSize = sampleSize)
        } finally {
            decoder.recycle()
        }
        checkNotNull(page) { "Failed to decode image" }

        val stripWidth = stripWidth(page, viewWidth)
        val strips = try {
            page.cutIntoStrips(stripWidth)
        } finally {
            page.recycle()
        }
        DecodeResult(image = WebtoonStrips(strips), isSampled = sampleSize > 1 || stripWidth < page.width)
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            return if (options.webtoonStrips) WebtoonStripsDecoder(result.source, options) else null
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }
}

/**
 * Strips keep the resolution the decoder's sample size leaves, so zooming in still shows the page's detail, unless
 * that would make the page take more than [MAX_PAGE_BYTES]. They are never narrower than the view, so the page stays
 * sharp unzoomed, and never wider than the GPU can draw.
 */
private fun stripWidth(page: Bitmap, viewWidth: Int): Int {
    val bytesPerPixel = page.rowBytes / page.width
    val budgetWidth = sqrt(MAX_PAGE_BYTES.toDouble() / bytesPerPixel * page.width / page.height).toInt()
    return maxOf(viewWidth, min(page.width, budgetWidth))
        .coerceAtMost(ImageUtil.hardwareBitmapThreshold)
        .coerceIn(1, page.width)
}

/** Strips of about [STRIP_BYTES] each, so each takes about as long to upload. Never returns this bitmap itself. */
private fun Bitmap.cutIntoStrips(stripWidth: Int): List<Bitmap> {
    val scale = stripWidth.toFloat() / width
    val matrix = Matrix().apply { setScale(scale, scale) }
    val stripRows = (STRIP_BYTES / (stripWidth * (rowBytes / width))).coerceAtLeast(1)
    val bandRows = (stripRows / scale).toInt().coerceIn(1, height)
    return (0..<height step bandRows).map { top ->
        val strip = Bitmap.createBitmap(this, 0, top, width, min(bandRows, height - top), matrix, true)
        // createBitmap returns a mutable bitmap despite its docs, and may return this bitmap itself; a copy is the
        // only way to an immutable bitmap of its own
        val config = strip.config ?: Bitmap.Config.ARGB_8888
        checkNotNull(strip.copy(config, false)).also { if (strip !== this) strip.recycle() }
    }
}

private const val STRIP_BYTES = 4 * 1024 * 1024

private const val MAX_PAGE_BYTES = 128 * 1024 * 1024

// Each decode holds the whole page until its strips are copied, and the long strip binds several pages at once
private val stripDecodes = Semaphore(1)
