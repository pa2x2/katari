package mihon.entry.interactions.translate.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/** Starts the background worker that drains the translation queue. */
internal interface EntryTranslateWorkController {
    fun start()
}

internal class DefaultEntryTranslateWorkController(
    private val context: Context,
) : EntryTranslateWorkController {
    override fun start() {
        val request = OneTimeWorkRequestBuilder<EntryTranslateJob>()
            .addTag(TAG)
            .build()
        // Work arriving after the running worker's last check for more is picked up by the appended one.
        WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val TAG = "EntryTranslator"
    }
}
