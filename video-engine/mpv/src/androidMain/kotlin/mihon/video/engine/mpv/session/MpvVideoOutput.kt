package mihon.video.engine.mpv.session

import android.view.Surface
import android.view.SurfaceHolder
import `is`.xyz.mpv.MPVLib
import logcat.LogPriority
import mihon.video.engine.spi.player.SurfaceViewOutput
import mihon.video.engine.spi.player.VideoOutput
import tachiyomi.core.common.util.system.logcat

/**
 * Gives mpv the surface of the [SurfaceViewOutput] the player is handed and follows it as it is created, resized and
 * destroyed. Without a surface mpv decodes to no output, which is how background playback keeps the sound going. A
 * surface's owner may destroy it as soon as its callback returns, so letting go of one waits for the mpv thread.
 *
 * [set], [release] and the surface callbacks come on the main thread; the rest runs on the mpv thread.
 */
internal class MpvVideoOutput(private val session: MpvInstance.Session) : SurfaceHolder.Callback {
    private var output: VideoOutput? = null
    private var holder: SurfaceHolder? = null

    /** Confined to the mpv thread. */
    private var attached: Surface? = null

    fun set(output: VideoOutput?) {
        if (output === this.output) return
        clear()
        this.output = output
        when (output) {
            null -> Unit
            is SurfaceViewOutput -> listen(output.view.holder)
            else -> logcat(LogPriority.WARN) { "mpv can't draw into a ${output::class.simpleName}" }
        }
    }

    /** Lets go of the surface once mpv has done everything posted before. */
    fun release() {
        holder?.removeCallback(this)
        holder = null
        output = null
        session.post { detachNow() }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val frame = holder.surfaceFrame
        attach(holder.surface, frame.width(), frame.height())
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        session.post { client ->
            if (attached != null) client.setString(SURFACE_SIZE, "${width}x$height")
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        detach()
    }

    private fun clear() {
        if (output == null) return
        holder?.removeCallback(this)
        holder = null
        output = null
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

    private fun attach(surface: Surface, width: Int, height: Int) {
        session.post { client ->
            // Destroyed while the session waited for the instance.
            if (!surface.isValid) return@post
            detachNow()
            MPVLib.attachSurface(surface)
            attached = surface
            if (width > 0 && height > 0) client.setString(SURFACE_SIZE, "${width}x$height")
            client.setString("force-window", "yes")
            client.setString("vo", VIDEO_OUTPUT)
        }
    }

    private fun detach() {
        if (!session.await(DETACH_TIMEOUT_MS) { detachNow() }) {
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
