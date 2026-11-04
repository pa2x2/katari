package mihon.video.engine.mpv.player

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import `is`.xyz.mpv.MPVLib
import logcat.LogPriority
import mihon.video.engine.mpv.session.MpvInstance
import tachiyomi.core.common.util.system.logcat

/**
 * Gives mpv the surface Media3 hands the player and follows it as it is created, resized and destroyed. Without a
 * surface mpv decodes to no output, which is how background playback keeps the sound going. A surface's owner may
 * destroy it as soon as its callback returns, so letting go of one waits for the mpv thread.
 *
 * [bind], [clear] and the surface callbacks come on the main thread; the rest runs on the mpv thread.
 */
internal class MpvVideoOutput(private val session: MpvInstance.MpvSession) : SurfaceHolder.Callback {
    private var output: Any? = null
    private var holder: SurfaceHolder? = null

    /** Confined to the mpv thread. */
    private var attached: Surface? = null

    fun bind(output: Any) {
        if (output === this.output) return
        clear(this.output)
        this.output = output
        when (output) {
            is SurfaceView -> listen(output.holder)
            is SurfaceHolder -> listen(output)
            is Surface -> attach(output, width = null, height = null)
            else -> logcat(LogPriority.WARN) { "mpv can't draw into a ${output::class.simpleName}" }
        }
    }

    fun clear(output: Any?) {
        if (output == null || output !== this.output) return
        holder?.removeCallback(this)
        holder = null
        this.output = null
        detach()
    }

    /** Lets go of the surface once mpv has done everything posted before. */
    fun release() {
        holder?.removeCallback(this)
        holder = null
        output = null
        session.post(::detachNow)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val frame = holder.surfaceFrame
        attach(holder.surface, frame.width(), frame.height())
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        session.post {
            if (attached != null) MPVLib.setPropertyString(SURFACE_SIZE, "${width}x$height")
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        detach()
    }

    private fun listen(holder: SurfaceHolder) {
        this.holder = holder
        holder.addCallback(this)
        val surface = holder.surface
        if (surface != null && surface.isValid) {
            val frame = holder.surfaceFrame
            attach(surface, frame.width(), frame.height())
        }
    }

    private fun attach(surface: Surface, width: Int?, height: Int?) {
        session.post {
            // Destroyed while the session waited for the instance.
            if (!surface.isValid) return@post
            detachNow()
            MPVLib.attachSurface(surface)
            attached = surface
            if (width != null && height != null && width > 0 && height > 0) {
                MPVLib.setPropertyString(SURFACE_SIZE, "${width}x$height")
            }
            MPVLib.setPropertyString("force-window", "yes")
            MPVLib.setPropertyString("vo", VIDEO_OUTPUT)
        }
    }

    private fun detach() {
        if (!session.await(DETACH_TIMEOUT_MS, ::detachNow)) {
            logcat(LogPriority.WARN) { "mpv didn't let go of its surface in ${DETACH_TIMEOUT_MS}ms" }
        }
    }

    private fun detachNow() {
        if (attached == null) return
        MPVLib.setPropertyString("vo", "null")
        MPVLib.setPropertyString("force-window", "no")
        MPVLib.detachSurface()
        attached = null
    }

    companion object {
        /** What mpv draws with once it has a surface; without one it has `vo=null`. */
        const val VIDEO_OUTPUT = "gpu"
        private const val SURFACE_SIZE = "android-surface-size"
        private const val DETACH_TIMEOUT_MS = 2_000L
    }
}
