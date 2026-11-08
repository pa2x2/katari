package eu.kanade.tachiyomi.data.coil

import androidx.compose.ui.unit.IntRect
import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import mihon.core.image.decode.ImageRegionDecoder
import mihon.core.image.decode.detectImageType
import mihon.core.image.progressive.ProgressiveImageFormat
import mihon.entry.interactions.manga.reader.image.profile.DisplayProfile
import okio.BufferedSource

/** A [Decoder] for the formats the platform's own image decoding lacks, through the app's native image decoder. */
internal class TachiyomiImageDecoder(private val resources: ImageSource, private val options: Options) : Decoder {

    override suspend fun decode(): DecodeResult {
        val decoder = resources.sourceOrNull()?.use {
            ImageRegionDecoder.open(it.inputStream(), options.cropBorders, DisplayProfile.bytes)
        }

        check(decoder != null && decoder.width > 0 && decoder.height > 0) { "Failed to initialize decoder" }

        val srcWidth = decoder.width
        val srcHeight = decoder.height

        val dstWidth = options.size.widthPx(options.scale) { srcWidth }
        val dstHeight = options.size.heightPx(options.scale) { srcHeight }

        val sampleSize = DecodeUtils.calculateInSampleSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = options.scale,
        )

        val bitmap = decoder.use { it.decode(IntRect(0, 0, srcWidth, srcHeight), sampleSize) }

        check(bitmap != null) { "Failed to decode image" }

        return DecodeResult(
            image = bitmap.toCoilImage(options),
            isSampled = sampleSize > 1,
        )
    }

    class Factory : Decoder.Factory {

        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            return if (isApplicable(result.source.source())) {
                TachiyomiImageDecoder(result.source, options)
            } else {
                null
            }
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            val peek = source.peek()
            peek.request(HEADER_BYTES)
            val header = peek.buffer.readByteArray(minOf(peek.buffer.size, HEADER_BYTES))
            return when (detectImageType(header)?.format) {
                ProgressiveImageFormat.AVIF, ProgressiveImageFormat.JXL, ProgressiveImageFormat.HEIF -> true
                else -> false
            }
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = this::class.hashCode()
    }
}

/** Enough of the file for the native decoder to tell its format. */
private const val HEADER_BYTES = 4L * 1024
