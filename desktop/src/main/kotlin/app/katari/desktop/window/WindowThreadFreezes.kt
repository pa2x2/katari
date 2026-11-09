package app.katari.desktop.window

import app.katari.telemetry.Telemetry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.awt.EventQueue
import kotlin.time.Duration.Companion.seconds

/**
 * Reports each time the window thread, which runs the app's input, composition and drawing, doesn't answer for
 * [FROZEN_AFTER]: a frozen window, which no exception tells of, as Android reports an app that stops responding. The
 * report carries the thread's stack as the freeze is found, and a freeze is reported once, however long it lasts.
 */
internal object WindowThreadFreezes {
    /** Watches until cancelled. Starts AWT, if nothing has. */
    suspend fun watch() {
        if (!Telemetry.isIncluded) return
        withContext(Dispatchers.IO) {
            var windowThread: Thread? = null
            while (true) {
                val answer = CompletableDeferred<Thread>()
                EventQueue.invokeLater { answer.complete(Thread.currentThread()) }
                if (withTimeoutOrNull(FROZEN_AFTER) { answer.await() } == null) {
                    windowThread?.let { Telemetry.recordException(WindowThreadFrozen(it.stackTrace)) }
                }
                windowThread = answer.await()
                delay(CHECK_INTERVAL)
            }
        }
    }
}

/** The window thread didn't answer for [FROZEN_AFTER]; the stack is the thread's at that moment. */
private class WindowThreadFrozen(stack: Array<StackTraceElement>) :
    Throwable("The window thread didn't answer for $FROZEN_AFTER") {
    init {
        stackTrace = stack
    }
}

// As long as Android waits on an app's input before it reports the app not responding
private val FROZEN_AFTER = 5.seconds
private val CHECK_INTERVAL = 1.seconds
