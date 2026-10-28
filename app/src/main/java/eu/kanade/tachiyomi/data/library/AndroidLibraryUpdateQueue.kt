package eu.kanade.tachiyomi.data.library

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.workDataOf
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob.Companion.toLibraryUpdateProgress
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mihon.feature.profiles.core.ProfileDatabase
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.update.model.LibraryUpdateStart
import tachiyomi.domain.library.update.model.LibraryUpdateState
import java.util.UUID

/**
 * The [LibraryUpdateQueue] as one WorkManager chain.
 *
 * WorkManager cancels everything queued behind a work that is cancelled, so nothing here cancels through it: a running
 * update is stopped from inside its worker, and a waiting one is marked to skip itself when its turn comes.
 */
@Inject
@SingleIn(AppScope::class)
class AndroidLibraryUpdateQueue(
    private val application: Application,
    private val profileDatabase: ProfileDatabase,
    private val activeProfileProvider: ActiveProfileProvider,
) : LibraryUpdateQueue {
    override val isOffered: Boolean = true

    private val enqueueLock = Mutex()

    private val skippedPreferences: SharedPreferences
        get() = application.getSharedPreferences(SKIPPED_FILE, Context.MODE_PRIVATE)

    override suspend fun startNow(category: Category?, sourceId: Long?, entryType: EntryType?): LibraryUpdateStart {
        return enqueue(
            profileId = activeProfileProvider.activeProfileId,
            input = workDataOf(
                LibraryUpdateJob.KEY_CATEGORY to category?.id,
                LibraryUpdateJob.KEY_SOURCE to sourceId,
                LibraryUpdateJob.KEY_ENTRY_TYPE to entryType?.name,
            ),
        )
    }

    override suspend fun startSelection(entryIds: Collection<Long>): LibraryUpdateStart {
        val profileId = activeProfileProvider.activeProfileId
        if (pendingUpdates().any { profileIdOf(it) == profileId }) return LibraryUpdateStart.AlreadyQueued
        val selectionName = LibraryUpdateSelectionFile(application).write(entryIds)
        return enqueue(profileId, workDataOf(LibraryUpdateJob.KEY_SELECTION to selectionName))
            .also { start ->
                if (start == LibraryUpdateStart.AlreadyQueued) {
                    LibraryUpdateSelectionFile(application).delete(selectionName)
                }
            }
    }

    override suspend fun startSkippedOfLatest(): LibraryUpdateStart {
        return enqueue(
            activeProfileProvider.activeProfileId,
            workDataOf(LibraryUpdateJob.KEY_SKIPPED_OF_LATEST to true),
        )
    }

    override suspend fun startAutomatic(profileId: Long): LibraryUpdateStart {
        return enqueue(profileId, Data.EMPTY, automatic = true)
    }

    override suspend fun stop() {
        stop(activeProfileProvider.activeProfileId)
    }

    override suspend fun stop(profileId: Long) {
        application.workManager.getWorkInfosForUniqueWork(UNIQUE_NAME).await()
            .filter { !it.state.isFinished && profileIdOf(it) == profileId }
            .forEach { info ->
                if (info.state != WorkInfo.State.RUNNING || !LibraryUpdateJob.stopRunning(info.id)) {
                    markSkipped(info.id)
                }
            }
    }

    override fun stateFlow(profileId: Long): Flow<LibraryUpdateState?> {
        return pendingUpdatesFlow()
            .map { pending ->
                val own = pending.firstOrNull { profileIdOf(it) == profileId } ?: return@map null
                if (own.state == WorkInfo.State.RUNNING) {
                    own.progress.toLibraryUpdateProgress()?.let(LibraryUpdateState::Running)
                } else {
                    pending.firstOrNull { it.state == WorkInfo.State.RUNNING }
                        ?.let(::profileIdOf)
                        ?.let { profileName(it) }
                        ?.let(LibraryUpdateState::Waiting)
                }
            }
            .distinctUntilChanged()
    }

    /** Profile of the update that starts after [running], or null when none waits. */
    fun nextProfileFlow(running: UUID): Flow<Long?> {
        return pendingUpdatesFlow()
            .map { pending -> pending.filter { it.id != running }.minByOrNull(::queuedAtOf)?.let(::profileIdOf) }
            .distinctUntilChanged()
    }

    /** True once for an update [stop] took out of the queue, which then ends without running. */
    @Synchronized
    internal fun consumeSkip(id: UUID): Boolean {
        val preferences = skippedPreferences
        val skipped = preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        if (id.toString() !in skipped) return false
        preferences.edit { putStringSet(SKIPPED_KEY, skipped - id.toString()) }
        return true
    }

    private suspend fun enqueue(
        profileId: Long,
        input: Data,
        automatic: Boolean = false,
    ): LibraryUpdateStart = enqueueLock.withLock {
        val pending = pendingUpdates()
        if (pending.any { profileIdOf(it) == profileId }) return LibraryUpdateStart.AlreadyQueued
        val request = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
            .addTag(TAG)
            .addTag("$PROFILE_TAG_PREFIX$profileId")
            .addTag("$QUEUED_AT_TAG_PREFIX${System.currentTimeMillis()}")
            .apply { if (automatic) addTag(TAG_AUTOMATIC) }
            .setInputData(Data.Builder().putAll(input).putLong(LibraryUpdateJob.KEY_PROFILE, profileId).build())
            .build()
        application.workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            .result.await()
        pending.maxByOrNull(::queuedAtOf)
            ?.let(::profileIdOf)
            ?.let { profileName(it) }
            ?.let(LibraryUpdateStart::Queued)
            ?: LibraryUpdateStart.Started
    }

    /** Updates running or waiting; one taken out of the queue only waits to end, so it isn't one of them. */
    private suspend fun pendingUpdates(): List<WorkInfo> {
        val skipped = skippedPreferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        return application.workManager.getWorkInfosForUniqueWork(UNIQUE_NAME).await()
            .filter { !it.state.isFinished && it.id.toString() !in skipped }
    }

    private fun pendingUpdatesFlow(): Flow<List<WorkInfo>> {
        return combine(application.workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_NAME), skippedFlow()) {
                infos,
                skipped,
            ->
            infos.filter { !it.state.isFinished && it.id.toString() !in skipped }
        }
    }

    private suspend fun profileName(profileId: Long): String? {
        return profileDatabase.getProfileById(profileId)?.name
    }

    private fun skippedFlow(): Flow<Set<String>> = callbackFlow {
        val preferences = skippedPreferences
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SKIPPED_KEY) trySend(preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        send(preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    @Synchronized
    private fun markSkipped(id: UUID) {
        val preferences = skippedPreferences
        val skipped = preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        preferences.edit { putStringSet(SKIPPED_KEY, skipped + id.toString()) }
    }

    private fun profileIdOf(info: WorkInfo): Long? {
        return info.tags.firstNotNullOfOrNull { tag -> tag.removePrefix(PROFILE_TAG_PREFIX).takeIf { it != tag } }
            ?.toLongOrNull()
    }

    private fun queuedAtOf(info: WorkInfo): Long {
        return info.tags.firstNotNullOfOrNull { tag -> tag.removePrefix(QUEUED_AT_TAG_PREFIX).takeIf { it != tag } }
            ?.toLongOrNull()
            ?: Long.MAX_VALUE
    }

    companion object {
        private const val UNIQUE_NAME = "LibraryUpdate-queue"
        private const val TAG = "LibraryUpdate"

        /** Marks updates the automatic schedule queued. */
        internal const val TAG_AUTOMATIC = "LibraryUpdate-auto"
        private const val PROFILE_TAG_PREFIX = "LibraryUpdate-profile-"

        /** Followed by when the update was queued, as WorkInfo doesn't tell the order of a chain. */
        private const val QUEUED_AT_TAG_PREFIX = "LibraryUpdate-queued-"

        private const val SKIPPED_FILE = "library_update_queue"
        private const val SKIPPED_KEY = "skipped"
    }
}
