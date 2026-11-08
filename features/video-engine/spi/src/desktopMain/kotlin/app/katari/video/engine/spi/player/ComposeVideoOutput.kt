package app.katari.video.engine.spi.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.skia.Image
import java.awt.Window

/**
 * Part of a Compose window as a player's output, which desktop engines draw into. The UI lays the part out, tells the
 * output its size in pixels with [resize], and draws [frame] across that size, under everything it shows there,
 * whenever [frameNumber] changes. The engine renders frames at that size through a [FrameSource] it attaches, at the
 * scale of the screen the [window] is on.
 */
class ComposeVideoOutput(val window: Window) : VideoOutput {
    private val frames = MutableStateFlow(0L)

    /** Counts up whenever there is something new to draw; read while drawing, so that drawing follows it. */
    val frameNumber: StateFlow<Long> = frames.asStateFlow()

    /** 0 until the UI is laid out. */
    @Volatile
    var width: Int = 0
        private set

    @Volatile
    var height: Int = 0
        private set

    @Volatile
    private var source: FrameSource? = null

    /** On the UI thread, whenever the part's size changes. */
    fun resize(width: Int, height: Int) {
        if (width == this.width && height == this.height) return
        this.width = width
        this.height = height
        source?.onResize()
    }

    /**
     * The picture to draw across the whole part now, or null for none yet. On the UI thread, while drawing; the image
     * is the source's, valid for this drawing.
     */
    fun frame(): Image? = source?.frame()

    /** Has [source] draw here from now on, in place of any other. */
    fun attach(source: FrameSource) {
        this.source = source
        frameReady()
    }

    /** Stops [source] drawing here, unless another took its place already. */
    fun detach(source: FrameSource) {
        if (this.source !== source) return
        this.source = null
        frameReady()
    }

    /** Asks the UI to draw again, for a new frame. From any thread. */
    fun frameReady() {
        frames.update { it + 1 }
    }

    /** What an engine draws an output with. */
    interface FrameSource {
        /** The frame to draw now; called on the UI thread while drawing. */
        fun frame(): Image?

        /** The output's [width] or [height] changed; called on the UI thread. */
        fun onResize()
    }
}
