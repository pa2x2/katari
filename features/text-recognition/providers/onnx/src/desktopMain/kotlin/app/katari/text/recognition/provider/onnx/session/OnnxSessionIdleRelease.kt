package app.katari.text.recognition.provider.onnx.session

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Frees the loaded models once none has run for [IDLE_MINUTES]: the desktop tells apps nothing about memory pressure,
 * and the models take hundreds of megabytes that would otherwise stay held until the app quits. Recognizing pages
 * again reloads them. [release] runs on a thread of its own, as closing sessions takes hundreds of milliseconds.
 */
internal class OnnxSessionIdleRelease(private val release: () -> Unit) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "onnx-session-release").apply { isDaemon = true }
    }
    private var pending: ScheduledFuture<*>? = null

    @Synchronized
    fun onInUseChanged(inUse: Boolean) {
        pending?.cancel(false)
        pending = if (inUse) null else scheduler.schedule(release, IDLE_MINUTES, TimeUnit.MINUTES)
    }

    private companion object {
        const val IDLE_MINUTES = 2L
    }
}
