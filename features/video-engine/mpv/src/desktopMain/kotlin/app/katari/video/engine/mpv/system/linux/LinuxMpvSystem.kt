package app.katari.video.engine.mpv.system.linux

import app.katari.video.engine.mpv.client.LibMpv
import app.katari.video.engine.mpv.render.MemoryFrames
import app.katari.video.engine.mpv.render.SoftwareFrames
import app.katari.video.engine.mpv.render.gl.EglFrames
import app.katari.video.engine.mpv.system.MpvSystem
import com.sun.jna.Pointer

/**
 * Linux: mpv renders with OpenGL off any window ([EglFrames]), where VA-API's frames need no copy and the GPU scales,
 * and with its software renderer where that can't start.
 */
internal object LinuxMpvSystem : MpvSystem {
    // The JVM formats its numbers itself, so nothing else in the process depends on the C library's locale
    override fun prepareProcess() {
        LinuxCLibrary.INSTANCE.setlocale(LinuxCLibrary.LC_NUMERIC, "C")
    }

    override fun createFrames(mpv: LibMpv, handle: Pointer): MemoryFrames? =
        EglFrames.create(mpv, handle) ?: SoftwareFrames.create(mpv, handle)
}
