package app.katari.video.engine.mpv.session

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import app.katari.video.engine.mpv.addon.MpvAddon
import app.katari.video.engine.mpv.client.MpvClient
import app.katari.video.engine.mpv.client.MpvLibClient
import app.katari.video.engine.mpv.client.MpvSession
import app.katari.video.engine.mpv.client.MpvSessionHost
import app.katari.video.engine.mpv.client.MpvValue
import app.katari.video.engine.mpv.client.mpvLogLevelOf
import app.katari.video.engine.mpv.client.toMpvEndFile
import app.katari.video.engine.spi.player.VideoOutput
import `is`.xyz.mpv.MPVLib
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The process's one mpv instance. `MPVLib` is a native singleton: any call while no instance exists ends the
 * process, and so does `create` while one does. So every call goes through the one thread here, which creates an
 * instance for one session at a time and destroys it when that session closes. A session opened while another
 * owns the instance waits for it: the next episode's player is made just before the previous one is released.
 *
 * Events arrive on mpv's own event thread and go to the owning session only. Destroying an instance joins that
 * thread, so a closed session gets no more of them.
 */
internal object MpvInstance {
    private val thread = HandlerThread("mpv").apply { start() }
    private val handler = Handler(thread.looper)

    /** Where the mpv thread runs, for helpers that must stay off the main thread. */
    val looper: Looper get() = thread.looper

    /** Written on the mpv thread while no event thread runs; read on the event thread. */
    @Volatile
    private var owner: Session? = null

    private val waiting = ArrayDeque<Session>()

    init {
        MPVLib.addObserver(EventRelay)
        MPVLib.addLogObserver(LogRelay)
    }

    /** A session for [host], whose instance needs the [addon]'s libraries loaded first. */
    fun open(context: Context, addon: MpvAddon, host: MpvSessionHost): MpvSession {
        val session = Session(context.applicationContext, addon, host)
        handler.post {
            waiting.addLast(session)
            startWaiting()
        }
        return session
    }

    private fun startWaiting() {
        while (owner == null) {
            val next = waiting.removeFirstOrNull() ?: return
            next.start()
        }
    }

    /** One owner of the instance, from [open] to [close]. */
    class Session internal constructor(
        private val context: Context,
        private val addon: MpvAddon,
        internal val host: MpvSessionHost,
    ) : MpvSession {
        private enum class Phase { Waiting, Live, Closed }

        // Confined to the mpv thread.
        private var phase = Phase.Waiting
        private val queued = mutableListOf<(MpvClient) -> Unit>()

        private val output = MpvVideoOutput(this)

        override fun post(block: (MpvClient) -> Unit) {
            handler.post {
                when (phase) {
                    Phase.Waiting -> queued += block
                    Phase.Live -> block(MpvLibClient)
                    Phase.Closed -> Unit
                }
            }
        }

        /**
         * Runs [block] now if the instance exists and waits up to [timeoutMs] for it, for work the caller must see
         * done before it returns, such as letting go of a surface. While the session still waits, nothing runs.
         */
        fun await(timeoutMs: Long, block: (MpvClient) -> Unit): Boolean {
            val done = CountDownLatch(1)
            handler.post {
                try {
                    if (phase == Phase.Live) block(MpvLibClient)
                } finally {
                    done.countDown()
                }
            }
            return done.await(timeoutMs, TimeUnit.MILLISECONDS)
        }

        override fun setVideoOutput(output: VideoOutput?) = this.output.set(output)

        override fun close(then: () -> Unit) {
            output.release()
            handler.post {
                when (phase) {
                    Phase.Waiting -> waiting.remove(this)
                    Phase.Live -> {
                        MPVLib.destroy()
                        owner = null
                    }
                    Phase.Closed -> Unit
                }
                phase = Phase.Closed
                queued.clear()
                then()
                startWaiting()
            }
        }

        internal fun start() {
            if (!addon.loadNow() || !host.beforeCreate()) {
                phase = Phase.Closed
                queued.clear()
                host.failedToStart()
                return
            }
            owner = this
            MPVLib.create(context)
            setAndroidOptions(MpvLibClient, context)
            host.configure(MpvLibClient)
            MPVLib.init()
            phase = Phase.Live
            host.started(MpvLibClient)
            queued.forEach { it(MpvLibClient) }
            queued.clear()
        }
    }

    private object LogRelay : MPVLib.LogObserver {
        override fun logMessage(prefix: String, level: Int, text: String) {
            val severity = mpvLogLevelOf(level) ?: return
            owner?.host?.onLogMessage(prefix, severity, text)
        }
    }

    private object EventRelay : MPVLib.EventObserver {
        override fun eventProperty(property: String) {
            owner?.host?.onPropertyChange(property, MpvValue.None)
        }

        override fun eventProperty(property: String, value: Long) {
            owner?.host?.onPropertyChange(property, MpvValue.Int64(value))
        }

        override fun eventProperty(property: String, value: Boolean) {
            owner?.host?.onPropertyChange(property, MpvValue.Flag(value))
        }

        override fun eventProperty(property: String, value: String) {
            owner?.host?.onPropertyChange(property, MpvValue.Text(value))
        }

        override fun eventProperty(property: String, value: Double) {
            owner?.host?.onPropertyChange(property, MpvValue.Double(value))
        }

        override fun event(eventId: Int) {
            val host = owner?.host ?: return
            when (eventId) {
                MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> host.onFileLoaded()
                // The event thread runs only while the instance exists, so the host may read from it.
                MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> host.onPlaybackRestart(MpvLibClient)
            }
        }

        override fun eventEndFile(endFile: MPVLib.EndFile) {
            val end = endFile.toMpvEndFile() ?: return
            owner?.host?.onEndFile(end)
        }
    }
}
