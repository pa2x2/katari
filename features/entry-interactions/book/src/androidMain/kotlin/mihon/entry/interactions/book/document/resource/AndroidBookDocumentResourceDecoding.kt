package mihon.entry.interactions.book.document.resource

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Typeface
import android.graphics.fonts.Font
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import com.caverock.androidsvg.SVG
import java.nio.Buffer
import java.nio.ByteBuffer
import android.graphics.fonts.FontFamily as PlatformFontFamily

/**
 * Reads what the image decoder doesn't, such as GIF, with BitmapFactory, draws vector images with AndroidSVG, and
 * builds fonts as Android typefaces.
 */
internal object AndroidBookDocumentResourceDecoding : BookDocumentResourceDecoding {
    override fun readRasterSize(bytes: ByteArray): IntSize? =
        readImageDecoderSize(bytes) ?: readBitmapFactorySize(bytes)

    override fun decodeRaster(bytes: ByteArray, sampleSize: Int): ProseImage =
        decodeWithImageDecoder(bytes, sampleSize) ?: decodeWithBitmapFactory(bytes, sampleSize)

    override fun readSvgSize(svg: String): ProseImageHeader.Vector = SVG.getFromString(svg).size()

    override fun renderSvg(svg: String, width: Int, height: Int): ProseImage {
        val document = SVG.getFromString(svg)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            document.setDocumentWidth(width.toFloat())
            document.setDocumentHeight(height.toFloat())
            document.renderToCanvas(Canvas(bitmap))
        }.toProseImage()
    }

    // Typeface.createFromFile returns the default typeface for data it can't read instead of failing, so a broken
    // font would pass validation and draw in the reader's font. Font.Builder rejects it.
    override fun fontFamily(sfnt: ByteArray): FontFamily {
        val font = Font.Builder(sfnt.toDirectBuffer()).build()
        val typeface = Typeface.CustomFallbackBuilder(PlatformFontFamily.Builder(font).build())
            // Glyphs the font lacks, such as another script, fall back to the system font as they would on the web.
            .setSystemFallback("sans-serif")
            .build()
        return FontFamily(typeface)
    }
}

internal fun readBitmapFactorySize(bytes: ByteArray): IntSize? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    return IntSize(bounds.outWidth, bounds.outHeight).takeIf { it.width > 0 && it.height > 0 }
}

internal fun decodeWithBitmapFactory(bytes: ByteArray, sampleSize: Int): ProseImage {
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("Unable to decode prose image")
    return bitmap.toProseImage()
}

private fun Bitmap.toProseImage() = ProseImage(asImageBitmap(), ::recycle)

private fun SVG.size() = ProseImageHeader.Vector(
    width = documentWidth.takeIf { it.isFinite() && it > 0f },
    height = documentHeight.takeIf { it.isFinite() && it > 0f },
)

// Font.Builder reads only direct buffers.
private fun ByteArray.toDirectBuffer(): ByteBuffer = ByteBuffer.allocateDirect(size).put(this).also(::rewind)

// Through Buffer: ByteBuffer's own covariant rewind() is missing before Android 14.
private fun rewind(buffer: Buffer) {
    buffer.rewind()
}
