package app.katari.video.engine.mpv.render

import app.katari.video.engine.mpv.client.LibMpv
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/** mpv's software renderer: scaling and colour conversion on the CPU, from frames hardware decoding copied back. */
internal class SoftwareFrames private constructor(
    private val mpv: LibMpv,
    override val renderContext: Pointer,
) : MemoryFrames {
    override val name = "software"

    override fun render(into: Pointer, stride: Int, width: Int, height: Int) {
        val params = RenderParams()
            .ints(RenderParams.SW_SIZE, width, height)
            .string(RenderParams.SW_FORMAT, "bgr0")
            .long(RenderParams.SW_STRIDE, stride.toLong())
            .pointer(RenderParams.SW_POINTER, into)
        mpv.renderContextRender(renderContext, params.build())
    }

    override fun free() = mpv.renderContextFree(renderContext)

    companion object {
        fun create(mpv: LibMpv, handle: Pointer): SoftwareFrames? {
            val result = PointerByReference()
            val status = mpv.renderContextCreate(
                result,
                handle,
                RenderParams().string(RenderParams.API_TYPE, "sw").build(),
            )
            if (status < 0) {
                logcat(LogPriority.ERROR) { "mpv's software renderer didn't start: ${mpv.errorString(status)}" }
                return null
            }
            return SoftwareFrames(mpv, result.value)
        }
    }
}
