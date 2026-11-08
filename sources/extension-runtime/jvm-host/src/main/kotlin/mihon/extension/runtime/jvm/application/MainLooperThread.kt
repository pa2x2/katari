package mihon.extension.runtime.jvm.application

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import kotlin.concurrent.thread

/**
 * The thread of Android's main `Looper`, which extensions and the stand-ins post to with `Handler(Looper.getMainLooper())`
 * and which hears settings changes, as Android's main thread does. Desktop's own UI runs on the AWT event thread, so
 * this one runs only what extensions hand it. [start] it once before any extension runs.
 */
object MainLooperThread : Executor {

    @Volatile
    private var handler: Handler? = null

    fun start() {
        synchronized(this) {
            check(handler == null) { "The main looper is already running" }
            val prepared = CountDownLatch(1)
            thread(name = "Android main looper", isDaemon = true) {
                Looper.prepareEnvironmentMainLooper()
                prepared.countDown()
                Looper.loop()
            }
            prepared.await()
            handler = Handler(Looper.getMainLooper())
        }
    }

    /** Runs [command] on the main looper's thread: at once when called there, else after what's already posted. */
    override fun execute(command: Runnable) {
        val handler = checkNotNull(handler) { "The main looper hasn't started" }
        if (Looper.myLooper() == handler.looper) command.run() else handler.post(command)
    }
}
