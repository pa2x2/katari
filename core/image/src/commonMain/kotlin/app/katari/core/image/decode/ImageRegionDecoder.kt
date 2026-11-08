package app.katari.core.image.decode

import androidx.compose.ui.unit.IntRect
import app.katari.core.image.PlatformBitmap
import java.io.InputStream

/**
 * Decodes regions of one encoded image, any of them and as often as asked, at a fraction of their resolution. The
 * native decoder holds the encoded image until [close].
 */
expect class ImageRegionDecoder : AutoCloseable {
    /** Width served, after borders are cropped when asked for. */
    val width: Int

    val height: Int

    /** The part of the encoded image served, in its own pixels: all of it, or what is left once borders are cropped. */
    val bounds: IntRect

    /**
     * Decodes [region], relative to [bounds]' top-left corner, divided by [sampleSize], a power of two. Returns null
     * when the region can't be decoded.
     */
    fun decode(region: IntRect, sampleSize: Int): PlatformBitmap?

    override fun close()

    companion object {
        /**
         * Reads the image's header from [stream], which it consumes and closes, or returns null for content it can't
         * decode. [cropBorders] serves the image without its plain borders; [displayProfile] is an ICC profile the
         * pixels are converted to.
         */
        fun open(
            stream: InputStream,
            cropBorders: Boolean = false,
            displayProfile: ByteArray? = null,
        ): ImageRegionDecoder?
    }
}
