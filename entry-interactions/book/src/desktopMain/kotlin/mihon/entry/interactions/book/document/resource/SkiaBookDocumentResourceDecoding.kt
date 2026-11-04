package mihon.entry.interactions.book.document.resource

import mihon.entry.interactions.book.document.resource.font.unpackWebFont
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.svg.SVGDOM
import java.io.File

/** Checks images and fonts with Skia, which draws them on desktop. */
internal object SkiaBookDocumentResourceDecoding : BookDocumentResourceDecoding {
    override fun requireDecodableImage(bytes: ByteArray, mediaType: String?) {
        if (mediaType == "image/svg+xml") {
            SVGDOM(Data.makeFromBytes(bytes)).use { svg ->
                requireNotNull(svg.root) { "Vector image has no SVG root" }
            }
            return
        }
        // Decoding every pixel, as the Android reader's check does, so a truncated image fails here
        Codec.makeFromData(Data.makeFromBytes(bytes)).use { codec ->
            require(codec.width in 1..MAX_IMAGE_DIMENSION && codec.height in 1..MAX_IMAGE_DIMENSION)
            require(codec.width.toLong() * codec.height.toLong() <= MAX_IMAGE_PIXELS)
            codec.readPixels().close()
        }
    }

    override fun requireLoadableFont(file: File) {
        val bytes = file.readBytes().let { unpackWebFont(it) ?: it }
        val typeface = FontMgr.default.makeFromData(Data.makeFromBytes(bytes))
        requireNotNull(typeface) { "Unable to read font" }.close()
    }
}

// The largest images the Android reader accepts
private const val MAX_IMAGE_DIMENSION = 32_768
private const val MAX_IMAGE_PIXELS = 64L * 1024L * 1024L
