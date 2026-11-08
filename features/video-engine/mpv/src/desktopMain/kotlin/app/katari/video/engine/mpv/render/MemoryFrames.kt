package app.katari.video.engine.mpv.render

import com.sun.jna.Pointer

/**
 * One way mpv renders frames into memory, through a render context of its own made on the render thread, where every
 * call comes.
 */
internal interface MemoryFrames {
    val renderContext: Pointer

    /** What renders, for the log. */
    val name: String

    /** Renders the current frame at [width] by [height] into [into], BGRX rows [stride] bytes apart, top row first. */
    fun render(into: Pointer, stride: Int, width: Int, height: Int)

    fun free()
}
