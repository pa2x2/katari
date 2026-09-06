package mihon.entry.interactions.download

import android.content.Context
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

internal class DefaultEntryDownloadWorkController(
    private val context: Context,
) : EntryDownloadWorkController {
    override fun start() {
        preferences.edit(commit = true) { putBoolean(KEY_EXECUTION_REQUESTED, true) }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<EntryDownloadJob>()
            .setConstraints(constraints)
            .addTag(TAG)
            .build()
        // Preserve requests arriving after the active worker has made its final idle check.
        // Appending also keeps active transfers intact; an empty successor simply drains no work.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(TAG, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    override fun stop() {
        preferences.edit(commit = true) { putBoolean(KEY_EXECUTION_REQUESTED, false) }
        WorkManager.getInstance(context).cancelUniqueWork(TAG)
    }

    override fun resumeIfRequested() {
        if (preferences.getBoolean(KEY_EXECUTION_REQUESTED, true)) start()
    }

    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private companion object {
        const val TAG = "EntryDownloader"
        const val PREFERENCES_NAME = "entry_download_execution"
        const val KEY_EXECUTION_REQUESTED = "requested"
    }
}
