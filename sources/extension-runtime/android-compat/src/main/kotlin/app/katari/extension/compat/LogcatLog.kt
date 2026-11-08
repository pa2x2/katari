package app.katari.extension.compat

import logcat.LogPriority
import logcat.asLog
import logcat.logcat

/**
 * Where the stand-in `android.util.Log` writes: the host's logcat loggers, under the caller's tag and with Android's
 * priority, as on a device. A throwable follows the message on its own lines, as `Log.e(tag, msg, tr)` prints it.
 */
object LogcatLog {
    @JvmStatic
    fun log(priority: Int, tag: String?, message: String?, throwable: Throwable?) {
        val logPriority = LogPriority.entries.firstOrNull { it.priorityInt == priority } ?: LogPriority.INFO
        logcat(tag.orEmpty(), logPriority) {
            if (throwable == null) message.orEmpty() else "${message.orEmpty()}\n${throwable.asLog()}"
        }
    }
}
