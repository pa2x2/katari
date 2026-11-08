package app.katari.extension.runtime.jvm.loading

import eu.kanade.tachiyomi.source.failure.isExtensionCodeError
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Keeps an error that an extension's own background work raised from ending the app, such as a coroutine it launched in
 * `GlobalScope` to fetch its filters failing to link one of its classes. No call of the app's is there to fail with
 * it, so it is logged and the thread, often one of the coroutine pool's that the app shares, goes on; the app's next
 * call into the extension meets the same error and fails with it shown.
 *
 * Only the errors [isExtensionCodeError] names count, and only with a frame of an extension's class on the stack.
 * Anything else goes to [next], the app's crash handling.
 */
class ExtensionUncaughtErrors private constructor(
    private val next: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, error: Throwable) {
        if (isExtensionCodeError(error) && error.ranExtensionCode()) {
            logcat(LogPriority.ERROR, error) { "An extension's background work failed on ${thread.name}" }
            return
        }
        if (next != null) {
            next.uncaughtException(thread, error)
        } else {
            // What the thread's group does without a default handler; asking it would come back here
            System.err.print("Exception in thread \"${thread.name}\" ")
            error.printStackTrace()
        }
    }

    private fun Throwable.ranExtensionCode(): Boolean =
        stackTrace.any { it.classLoaderName == ExtensionJarClassLoader.NAME }

    companion object {
        /** Puts this in front of the default handler installed now. */
        fun install() {
            Thread.setDefaultUncaughtExceptionHandler(
                ExtensionUncaughtErrors(Thread.getDefaultUncaughtExceptionHandler()),
            )
        }
    }
}
