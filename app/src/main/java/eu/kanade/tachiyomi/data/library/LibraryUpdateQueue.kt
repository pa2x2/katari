package eu.kanade.tachiyomi.data.library

import android.content.Context
import androidx.core.content.edit
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob.Companion.toLibraryUpdateProgress
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.app.di.appGraph
import tachiyomi.domain.category.model.Category
import java.util.UUID

/**
 * Every library update, of every profile, waits its turn in one WorkManager chain, so profiles never update at the same
 * time and opening another profile leaves a started update in place. A profile has at most one update running or
 * waiting.
 *
 * WorkManager cancels everything queued behind a work that is cancelled, so nothing here cancels through it: a running
 * update is stopped from inside its worker, and a waiting one is marked to skip itself when its turn comes.
 */
object LibraryUpdateQueue {
    private const val UNIQUE_NAME = "LibraryUpdate-queue"
    internal const val TAG = "LibraryUpdate"

    /** Marks updates the automatic schedule queued. */
    internal const val TAG_AUTOMATIC = "LibraryUpdate-auto"
    private const val PROFILE_TAG_PREFIX = "LibraryUpdate-profile-"

    private const val SKIPPED_FILE = "library_update_queue"
    private const val SKIPPED_KEY = "skipped"

    private val enqueueLock = Mutex()

    /** Checks what the update rules allow, limited to one page of the library when a scope is given. */
    suspend fun startNow(
        context: Context,
        category: Category? = null,
        sourceId: Long? = null,
        entryType: EntryType? = null,
    ): Boolean {
        return enqueue(
            context = context,
            profileId = context.activeProfileId,
            input = workDataOf(
                LibraryUpdateJob.KEY_CATEGORY to category?.id,
                LibraryUpdateJob.KEY_SOURCE to sourceId,
                LibraryUpdateJob.KEY_ENTRY_TYPE to entryType?.name,
            ),
        )
    }

    /** Checks the picked entries whatever the rules say, leaving out only entries set to Never. */
    suspend fun startSelection(context: Context, entryIds: Collection<Long>): Boolean {
        val profileId = context.activeProfileId
        if (hasUpdate(context.workManager, profileId)) return false
        val selectionName = LibraryUpdateSelectionFile(context).write(entryIds)
        return enqueue(context, profileId, workDataOf(LibraryUpdateJob.KEY_SELECTION to selectionName))
            .also { queued -> if (!queued) LibraryUpdateSelectionFile(context).delete(selectionName) }
    }

    suspend fun startSkippedOfLatest(context: Context): Boolean {
        return enqueue(
            context,
            context.activeProfileId,
            workDataOf(LibraryUpdateJob.KEY_SKIPPED_OF_LATEST to true),
        )
    }

    /** Queues the automatic update of [profileId], unless that profile already has an update running or waiting. */
    internal suspend fun startAutomatic(context: Context, profileId: Long): Boolean {
        return enqueue(context, profileId, Data.EMPTY, automatic = true)
    }

    /** Stops the update of the active profile, running or waiting; the next one in the queue goes ahead. */
    suspend fun stop(context: Context) {
        stop(context, context.activeProfileId)
    }

    /** Stops whichever update is running, as the progress notification shows it; the next one goes ahead. */
    suspend fun stopRunning(context: Context) {
        context.workManager.getWorkInfosForUniqueWork(UNIQUE_NAME).await()
            .filter { it.state == WorkInfo.State.RUNNING }
            .forEach { info -> if (!LibraryUpdateJob.stopRunning(info.id)) markSkipped(context, info.id) }
    }

    /** Stops the update of [profileId], running or waiting; the next one in the queue goes ahead. */
    suspend fun stop(context: Context, profileId: Long) {
        context.workManager.getWorkInfosForUniqueWork(UNIQUE_NAME).await()
            .filter { !it.state.isFinished && profileIdOf(it) == profileId }
            .forEach { info ->
                if (info.state != WorkInfo.State.RUNNING || !LibraryUpdateJob.stopRunning(info.id)) {
                    markSkipped(context, info.id)
                }
            }
    }

    /**
     * Progress of the running library update, or null when none runs. An update shows up once it has decided to go
     * ahead.
     */
    fun progressFlow(context: Context): Flow<LibraryUpdateProgress?> {
        return context.workManager.getWorkInfosByTagFlow(TAG)
            .map { workInfos ->
                workInfos
                    .filter { it.state == WorkInfo.State.RUNNING }
                    .firstNotNullOfOrNull { it.progress.toLibraryUpdateProgress() }
            }
            .distinctUntilChanged()
    }

    /** True once for an update [stop] took out of the queue, which then ends without running. */
    @Synchronized
    internal fun consumeSkip(context: Context, id: UUID): Boolean {
        val preferences = context.getSharedPreferences(SKIPPED_FILE, Context.MODE_PRIVATE)
        val skipped = preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        if (id.toString() !in skipped) return false
        preferences.edit { putStringSet(SKIPPED_KEY, skipped - id.toString()) }
        return true
    }

    private suspend fun enqueue(
        context: Context,
        profileId: Long,
        input: Data,
        automatic: Boolean = false,
    ): Boolean = enqueueLock.withLock {
        val workManager = context.workManager
        if (hasUpdate(workManager, profileId)) return false
        val request = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
            .addTag(TAG)
            .addTag("$PROFILE_TAG_PREFIX$profileId")
            .apply { if (automatic) addTag(TAG_AUTOMATIC) }
            .setInputData(Data.Builder().putAll(input).putLong(LibraryUpdateJob.KEY_PROFILE, profileId).build())
            .build()
        workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request).result.await()
        true
    }

    private suspend fun hasUpdate(workManager: WorkManager, profileId: Long): Boolean {
        return workManager.getWorkInfosForUniqueWork(UNIQUE_NAME).await()
            .any { !it.state.isFinished && profileIdOf(it) == profileId }
    }

    @Synchronized
    private fun markSkipped(context: Context, id: UUID) {
        val preferences = context.getSharedPreferences(SKIPPED_FILE, Context.MODE_PRIVATE)
        val skipped = preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        preferences.edit { putStringSet(SKIPPED_KEY, skipped + id.toString()) }
    }

    internal fun profileIdOf(info: WorkInfo): Long? {
        return info.tags.firstNotNullOfOrNull { tag -> tag.removePrefix(PROFILE_TAG_PREFIX).takeIf { it != tag } }
            ?.toLongOrNull()
    }

    private val Context.activeProfileId: Long
        get() = appGraph.activeProfileProvider.activeProfileId
}
