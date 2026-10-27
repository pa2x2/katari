package eu.kanade.tachiyomi.data.library

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.workDataOf
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.guava.await
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.service.MetadataUpdateRunner

@Inject
class AndroidMetadataUpdateRunner(
    private val application: Application,
    private val activeProfileProvider: ActiveProfileProvider,
) : MetadataUpdateRunner {
    override suspend fun startNow(): Boolean {
        val wm = application.workManager
        if (wm.isRunning(TAG)) {
            // Already running either as a scheduled or manual job
            return false
        }
        val request = OneTimeWorkRequestBuilder<MetadataUpdateJob>()
            .addTag(TAG)
            .addTag(WORK_NAME_MANUAL)
            .setInputData(workDataOf(MetadataUpdateJob.KEY_PROFILE to activeProfileProvider.activeProfileId))
            .build()
        wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)

        return true
    }

    override suspend fun stop() {
        val wm = application.workManager
        val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
            .addStates(listOf(WorkInfo.State.RUNNING))
            .build()
        wm.getWorkInfos(workQuery).await()
            // Should only return one work but just in case
            .forEach {
                wm.cancelWorkById(it.id)
            }
    }

    private companion object {
        const val TAG = "MetadataUpdate"
        const val WORK_NAME_MANUAL = "MetadataUpdate"
    }
}
