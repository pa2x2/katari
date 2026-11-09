package app.katari.telemetry.freeze

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.posthog.PostHog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Reports each time Android ended the app because it stopped responding, which PostHog's error tracking doesn't see.
 * Android keeps a record of each, with the stacks of the app's threads, and they are read on the next start. The records
 * came with Android 11, so earlier versions report none.
 */
internal class ApplicationNotRespondingReports(private val application: Application) {
    fun reportNew(scope: CoroutineScope) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        scope.launch { report() }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun report() {
        val activityManager = application.getSystemService<ActivityManager>() ?: return
        val reported = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val reportedUntil = reported.getLong(REPORTED_UNTIL_KEY, 0)
        val records = activityManager.getHistoricalProcessExitReasons(application.packageName, 0, 0)
            .filter { it.reason == ApplicationExitInfo.REASON_ANR && it.timestamp > reportedUntil }
            .sortedBy { it.timestamp }
        records.forEach(::capture)
        records.lastOrNull()?.let { newest -> reported.edit { putLong(REPORTED_UNTIL_KEY, newest.timestamp) } }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun capture(record: ApplicationExitInfo) {
        val trace = record.traceInputStream?.use { it.bufferedReader().readText() }
        val freeze = ApplicationNotResponding(record.description, trace?.let(::mainThreadStack).orEmpty())
        PostHog.captureException(freeze, mapOf(EXCEPTION_LEVEL to "fatal"))
    }
}

/** Android ended the app because it stopped responding; the stack is the main thread's at that moment. */
private class ApplicationNotResponding(description: String?, mainThreadStack: List<StackTraceElement>) :
    Throwable(description) {
    init {
        stackTrace = mainThreadStack.toTypedArray()
    }
}

private const val PREFERENCES_NAME = "telemetry"
private const val REPORTED_UNTIL_KEY = "anr_reported_until"
private const val EXCEPTION_LEVEL = "\$exception_level"
