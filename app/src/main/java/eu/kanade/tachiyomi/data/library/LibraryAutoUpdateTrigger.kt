package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI

/** Runs on a profile's [LibraryUpdateSchedule] and queues that profile's automatic update. */
class LibraryAutoUpdateTrigger(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val profileId = inputData.getLong(KEY_PROFILE, -1L).takeIf { it >= 0L } ?: return Result.failure()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // Before Android 9 the schedule's constraints can't require Wi-Fi.
            val preferences = LibraryPreferences(context.appGraph.profileStore.profileStore(profileId))
            if (DEVICE_ONLY_ON_WIFI in preferences.autoUpdateDeviceRestrictions.get() && !context.isConnectedToWifi()) {
                logcat(LogPriority.INFO) { "Skipping automatic library update because device is not on Wi-Fi" }
                return Result.retry()
            }
        }
        if (LibraryUpdateQueue.startAutomatic(context, profileId) == LibraryUpdateStart.AlreadyQueued) {
            logcat(LogPriority.INFO) {
                "Skipping automatic library update of profile $profileId, one is already queued"
            }
        }
        return Result.success()
    }

    companion object {
        internal const val KEY_PROFILE = "profile"
    }
}
