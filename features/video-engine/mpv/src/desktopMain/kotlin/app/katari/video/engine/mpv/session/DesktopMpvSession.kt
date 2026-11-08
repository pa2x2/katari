package app.katari.video.engine.mpv.session

import app.katari.video.engine.mpv.client.JnaMpvClient
import app.katari.video.engine.mpv.client.LibMpv
import app.katari.video.engine.mpv.client.MpvClient
import app.katari.video.engine.mpv.client.MpvEventPump
import app.katari.video.engine.mpv.client.MpvSession
import app.katari.video.engine.mpv.client.MpvSessionHost
import app.katari.video.engine.mpv.client.MpvUnavailableException
import app.katari.video.engine.mpv.client.libMpv
import app.katari.video.engine.mpv.render.MpvRenderer
import app.katari.video.engine.spi.player.ComposeVideoOutput
import app.katari.video.engine.spi.player.VideoOutput
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicInteger

/**
 * One player's mpv instance on the desktop, where libmpv makes as many instances as asked: the next episode's player
 * and the seek preview each have their own. Each session drives its instance from a thread of its own, and its events
 * come on an event thread of the instance's ([MpvEventPump]).
 *
 * mpv draws through an [MpvRenderer] made before the host's first command, so that the first file already opens with
 * an output that shows VA-API's frames without a copy; its picture goes to whichever [ComposeVideoOutput] is set. Where
 * no renderer can start, mpv decodes with `vo=null` and nothing is drawn.
 */
internal class DesktopMpvSession(private val host: MpvSessionHost) : MpvSession {
    private val executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "mpv-${sessions.incrementAndGet()}").apply { isDaemon = true }
    }

    private enum class Phase { Waiting, Live, Closed }

    // Confined to the mpv thread.
    private var phase = Phase.Waiting
    private val queued = mutableListOf<(MpvClient) -> Unit>()
    private var mpv: LibMpv? = null
    private var client: JnaMpvClient? = null
    private var events: MpvEventPump? = null
    private var output: ComposeVideoOutput? = null
    private var renderer: MpvRenderer? = null

    init {
        onMpvThread(::start)
    }

    override fun post(block: (MpvClient) -> Unit) = onMpvThread {
        when (phase) {
            Phase.Waiting -> queued += block
            Phase.Live -> client?.let(block)
            Phase.Closed -> Unit
        }
    }

    override fun setVideoOutput(output: VideoOutput?) = onMpvThread {
        if (output != null && output !is ComposeVideoOutput) {
            logcat(LogPriority.WARN) { "mpv can't draw into a ${output::class.simpleName}" }
        }
        this.output = output as? ComposeVideoOutput
        showOutput()
    }

    override fun close(then: () -> Unit) = onMpvThread {
        output = null
        renderer?.let { renderer ->
            client?.setString("vo", "null")
            renderer.release()
        }
        renderer = null
        if (phase == Phase.Live) {
            events?.stop()
            client?.let { mpv?.terminateDestroy(it.handle) }
        }
        events = null
        client = null
        phase = Phase.Closed
        queued.clear()
        then()
        executor.shutdown()
    }

    private fun start() {
        if (phase != Phase.Waiting) return
        val mpv = try {
            libMpv()
        } catch (e: MpvUnavailableException) {
            logcat(LogPriority.ERROR, e) { "mpv can't play" }
            return failedToStart()
        }
        if (!host.beforeCreate()) return failedToStart()
        val handle = mpv.create() ?: return failedToStart()
        val client = JnaMpvClient(mpv, handle)
        setDesktopOptions(client)
        host.configure(client)
        mpv.requestLogMessages(handle, LOG_LEVEL)
        val initialized = mpv.initialize(handle)
        if (initialized < 0) {
            logcat(LogPriority.ERROR) { "mpv didn't start: ${mpv.errorString(initialized)}" }
            mpv.terminateDestroy(handle)
            return failedToStart()
        }
        this.mpv = mpv
        this.client = client
        events = MpvEventPump(mpv, client, host, "${Thread.currentThread().name}-events").also { it.start() }
        renderer = MpvRenderer(mpv, handle).takeIf { it.start() }
        if (renderer != null) client.setString("vo", RENDERED_OUTPUT)
        phase = Phase.Live
        host.started(client)
        queued.forEach { it(client) }
        queued.clear()
        showOutput()
    }

    private fun failedToStart() {
        phase = Phase.Closed
        queued.clear()
        host.failedToStart()
    }

    private fun showOutput() {
        val renderer = renderer ?: return
        output?.let(renderer::attach) ?: renderer.detach()
    }

    private fun onMpvThread(block: () -> Unit) {
        try {
            executor.execute(block)
        } catch (_: RejectedExecutionException) {
            // Closed: nothing is left to run it on.
        }
    }

    private companion object {
        val sessions = AtomicInteger()

        /** mpv's output when it draws through a render context; without one it has `vo=null`. */
        const val RENDERED_OUTPUT = "libmpv"

        /**
         * The least severe of mpv's messages the player hears: it keeps errors, for a failure's details, and puts
         * warnings in the app's log too, where a stream's broken timestamps or cut-off downloads show.
         */
        const val LOG_LEVEL = "warn"
    }
}
