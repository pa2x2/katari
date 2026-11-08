package app.katari.video.engine.mpv.render

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

/**
 * Three frames of [width] by [height] in Skia's memory, between the thread mpv renders on and the UI that draws them:
 * the render thread never writes the frame that is ready nor the one last taken, so the UI draws a whole frame. Each
 * frame is a bitmap marked immutable, so an image of it shares its pixels instead of copying them, and keeps them
 * alive for as long as anything draws it, even after the slots are gone.
 */
internal class FrameSlots(val width: Int, val height: Int) {
    private val bitmaps = List(SLOTS) {
        val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
        Bitmap().apply {
            check(allocPixels(info)) { "No memory for a frame of ${info.width}x${info.height}" }
            setImmutable()
        }
    }
    private var ready = -1
    private var taken = -1
    private var sequence = 0L

    /** A slot the render thread may write; [bitmap] gives its memory. */
    @Synchronized
    fun acquire(): Int = (0 until SLOTS).first { it != ready && it != taken }

    fun bitmap(slot: Int): Bitmap = bitmaps[slot]

    /**
     * [slot] holds a whole new frame. Its pixels changed under an immutable bitmap, so Skia is told: a GPU canvas keeps
     * the texture it uploaded an image to under the pixels' generation, and would draw the old frame again.
     */
    @Synchronized
    fun publish(slot: Int) {
        bitmaps[slot].notifyPixelsChanged()
        ready = slot
        sequence++
    }

    /** The newest frame and its sequence number, which grows with each new one; null before the first. */
    @Synchronized
    fun take(): Pair<Bitmap, Long>? {
        if (ready >= 0) {
            taken = ready
            ready = -1
        }
        return if (taken >= 0) bitmaps[taken] to sequence else null
    }

    /**
     * Lets go of the bitmaps; images made of them keep their pixels. On the UI thread, once nothing takes from here.
     */
    fun close() = bitmaps.forEach(Bitmap::close)

    private companion object {
        const val SLOTS = 3
    }
}
