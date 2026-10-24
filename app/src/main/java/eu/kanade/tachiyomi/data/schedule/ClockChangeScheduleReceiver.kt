package eu.kanade.tachiyomi.data.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.library.LibraryUpdateSchedule
import eu.kanade.tachiyomi.data.statistics.StatisticsRecapNotificationJob
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
        context.appGraph.appScope.launch {
            try {
                BackupCreateJob.setupTask(context)
                LibraryUpdateSchedule.scheduleAll(context)
                StatisticsRecapNotificationJob.setupTask(context)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
