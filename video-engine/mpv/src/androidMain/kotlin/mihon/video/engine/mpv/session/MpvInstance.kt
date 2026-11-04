package mihon.video.engine.mpv.session

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import `is`.xyz.mpv.MPVLib
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The process's one mpv instance. `MPVLib` is a native singleton: any call while no instance exists ends the
 * process, and so does `create` while one does. So every call goes through the one thread here, which creates an
 * instance for one [MpvSession] at a time and destroys it when that session closes. A session opened while another
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
    private var owner: MpvSession? = null

    private val waiting = ArrayDeque<MpvSession>()

    init {
        MPVLib.addObserver(EventRelay)
        MPVLib.addLogObserver(LogRelay)
    }

    fun open(context: Context, host: MpvSessionHost): MpvSession {
        val session = MpvSession(context.applicationContext, host)
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
    class MpvSession internal constructor(
        private val context: Context,
        internal val host: MpvSessionHost,
    ) {
        private enum class Phase { Waiting, Live, Closed }

        // Confined to the mpv thread.
        private var phase = Phase.Waiting
        private val queued = mutableListOf<() -> Unit>()

        /** Runs [block] on the mpv thread with the instance, once it exists; dropped if the session closes first. */
        fun post(block: () -> Unit) {
            handler.post {
                when (phase) {
                    Phase.Waiting -> queued += block
                    Phase.Live -> block()
                    Phase.Closed -> Unit
                }
            }
        }

        /**
         * Runs [block] now if the instance exists and waits up to [timeoutMs] for it, for work the caller must see
         * done before it returns, such as letting go of a surface. While the session still waits, nothing runs.
         */
        fun await(timeoutMs: Long, block: () -> Unit): Boolean {
            val done = CountDownLatch(1)
            handler.post {
                try {
                    if (phase == Phase.Live) block()
                } finally {
                    done.countDown()
                }
            }
            return done.await(timeoutMs, TimeUnit.MILLISECONDS)
        }

        /** Destroys the instance, or stops waiting for it, then runs [then] on the mpv thread. */
        fun close(then: () -> Unit = {}) {
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
            if (!host.beforeCreate()) {
                phase = Phase.Closed
                queued.clear()
                host.failedToStart()
                return
            }
            owner = this
            MPVLib.create(context)
            host.configure()
            MPVLib.init()
            phase = Phase.Live
            host.started()
            queued.forEach { it() }
            queued.clear()
        }
    }

    private object LogRelay : MPVLib.LogObserver {
        override fun logMessage(prefix: String, level: Int, text: String) {
            owner?.host?.logMessage(prefix, level, text)
        }
    }

    private object EventRelay : MPVLib.EventObserver {
        override fun eventProperty(property: String) {
            owner?.host?.eventProperty(property)
        }

        override fun eventProperty(property: String, value: Long) {
            owner?.host?.eventProperty(property, value)
        }

        override fun eventProperty(property: String, value: Boolean) {
            owner?.host?.eventProperty(property, value)
        }

        override fun eventProperty(property: String, value: String) {
            owner?.host?.eventProperty(property, value)
        }

        override fun eventProperty(property: String, value: Double) {
            owner?.host?.eventProperty(property, value)
        }

        override fun event(eventId: Int) {
            owner?.host?.event(eventId)
        }

        override fun eventEndFile(endFile: MPVLib.EndFile) {
            owner?.host?.eventEndFile(endFile)
        }
    }
}

/**
 * What an [MpvInstance.MpvSession] runs its instance for. The lifecycle calls come on the mpv thread; the event and
 * log calls on mpv's event thread, which must not block.
 */
internal interface MpvSessionHost : MPVLib.EventObserver, MPVLib.LogObserver {
    /**
     * Gets ready for the instance, such as by loading the add-on's libraries; false when it can't run, which ends
     * the session.
     */
    fun beforeCreate(): Boolean

    /** Sets the new instance's options, before it initialises. */
    fun configure()

    /** The instance runs: observe its properties. Anything posted while the session waited runs right after. */
    fun started()

    fun failedToStart()
}
