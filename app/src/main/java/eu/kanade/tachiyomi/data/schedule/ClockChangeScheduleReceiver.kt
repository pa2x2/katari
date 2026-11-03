package eu.kanade.tachiyomi.data.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat

/**
 * Sets up the periodic jobs again after the clock is changed, so that a clock set back doesn't hold off automatic
 * backups, library updates and recaps until it catches up again.
 */
class ClockChangeScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIME_CHANGED) return

        val pendingResult = goAsync()
        val graph = context.appGraph
        graph.appScope.launch {
            try {
                graph.backupCreateSchedule.setupTask()
                graph.libraryUpdateSchedule.scheduleAll()
                graph.statisticsRecapSchedule.setupTask()
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
