package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.workDataOf
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

    /** Followed by when the update was queued, as WorkInfo doesn't tell the order of a chain. */
    private const val QUEUED_AT_TAG_PREFIX = "LibraryUpdate-queued-"

    private const val SKIPPED_FILE = "library_update_queue"
    private const val SKIPPED_KEY = "skipped"

    private val enqueueLock = Mutex()

    /** Checks what the update rules allow, limited to one page of the library when a scope is given. */
    suspend fun startNow(
        context: Context,
        category: Category? = null,
        sourceId: Long? = null,
        entryType: EntryType? = null,
    ): LibraryUpdateStart {
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
    suspend fun startSelection(context: Context, entryIds: Collection<Long>): LibraryUpdateStart {
        val profileId = context.activeProfileId
        if (pendingUpdates(context).any { profileIdOf(it) == profileId }) return LibraryUpdateStart.AlreadyQueued
        val selectionName = LibraryUpdateSelectionFile(context).write(entryIds)
        return enqueue(context, profileId, workDataOf(LibraryUpdateJob.KEY_SELECTION to selectionName))
            .also { start ->
                if (start == LibraryUpdateStart.AlreadyQueued) LibraryUpdateSelectionFile(context).delete(selectionName)
            }
    }

    suspend fun startSkippedOfLatest(context: Context): LibraryUpdateStart {
        return enqueue(
            context,
            context.activeProfileId,
            workDataOf(LibraryUpdateJob.KEY_SKIPPED_OF_LATEST to true),
        )
    }

    /** Queues the automatic update of [profileId], unless that profile already has an update running or waiting. */
    internal suspend fun startAutomatic(context: Context, profileId: Long): LibraryUpdateStart {
        return enqueue(context, profileId, Data.EMPTY, automatic = true)
    }

    /** Stops the update of the active profile, running or waiting; the next one in the queue goes ahead. */
    suspend fun stop(context: Context) {
        stop(context, context.activeProfileId)
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
     * The update of [profileId], or null when it has none. A running update shows up once it has decided to go ahead,
     * and a waiting one only while another profile's update runs.
     */
    fun stateFlow(context: Context, profileId: Long): Flow<LibraryUpdateState?> {
        return pendingUpdatesFlow(context)
            .map { pending ->
                val own = pending.firstOrNull { profileIdOf(it) == profileId } ?: return@map null
                if (own.state == WorkInfo.State.RUNNING) {
                    own.progress.toLibraryUpdateProgress()?.let(LibraryUpdateState::Running)
                } else {
                    pending.firstOrNull { it.state == WorkInfo.State.RUNNING }
                        ?.let(::profileIdOf)
                        ?.let { profileName(context, it) }
                        ?.let(LibraryUpdateState::Waiting)
                }
            }
            .distinctUntilChanged()
    }

    /** Profile of the update that starts after [running], or null when none waits. */
    fun nextProfileFlow(context: Context, running: UUID): Flow<Long?> {
        return pendingUpdatesFlow(context)
            .map { pending -> pending.filter { it.id != running }.minByOrNull(::queuedAtOf)?.let(::profileIdOf) }
            .distinctUntilChanged()
    }

    /** True once for an update [stop] took out of the queue, which then ends without running. */
    @Synchronized
    internal fun consumeSkip(context: Context, id: UUID): Boolean {
        val preferences = context.skippedPreferences
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
    ): LibraryUpdateStart = enqueueLock.withLock {
        val pending = pendingUpdates(context)
        if (pending.any { profileIdOf(it) == profileId }) return LibraryUpdateStart.AlreadyQueued
        val request = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
            .addTag(TAG)
            .addTag("$PROFILE_TAG_PREFIX$profileId")
            .addTag("$QUEUED_AT_TAG_PREFIX${System.currentTimeMillis()}")
            .apply { if (automatic) addTag(TAG_AUTOMATIC) }
            .setInputData(Data.Builder().putAll(input).putLong(LibraryUpdateJob.KEY_PROFILE, profileId).build())
            .build()
        context.workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request).result.await()
        pending.maxByOrNull(::queuedAtOf)
            ?.let(::profileIdOf)
            ?.let { profileName(context, it) }
            ?.let(LibraryUpdateStart::Queued)
            ?: LibraryUpdateStart.Started
    }

    /** Updates running or waiting; one taken out of the queue only waits to end, so it isn't one of them. */
    private suspend fun pendingUpdates(context: Context): List<WorkInfo> {
        val skipped = context.skippedPreferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        return context.workManager.getWorkInfosForUniqueWork(UNIQUE_NAME).await()
            .filter { !it.state.isFinished && it.id.toString() !in skipped }
    }

    private fun pendingUpdatesFlow(context: Context): Flow<List<WorkInfo>> {
        return combine(context.workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_NAME), skippedFlow(context)) {
                infos,
                skipped,
            ->
            infos.filter { !it.state.isFinished && it.id.toString() !in skipped }
        }
    }

    private suspend fun profileName(context: Context, profileId: Long): String? {
        return context.appGraph.profileDatabase.getProfileById(profileId)?.name
    }

    private fun skippedFlow(context: Context): Flow<Set<String>> = callbackFlow {
        val preferences = context.skippedPreferences
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SKIPPED_KEY) trySend(preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        send(preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    @Synchronized
    private fun markSkipped(context: Context, id: UUID) {
        val preferences = context.skippedPreferences
        val skipped = preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        preferences.edit { putStringSet(SKIPPED_KEY, skipped + id.toString()) }
    }

    internal fun profileIdOf(info: WorkInfo): Long? {
        return info.tags.firstNotNullOfOrNull { tag -> tag.removePrefix(PROFILE_TAG_PREFIX).takeIf { it != tag } }
            ?.toLongOrNull()
    }

    private fun queuedAtOf(info: WorkInfo): Long {
        return info.tags.firstNotNullOfOrNull { tag -> tag.removePrefix(QUEUED_AT_TAG_PREFIX).takeIf { it != tag } }
            ?.toLongOrNull()
            ?: Long.MAX_VALUE
    }

    private val Context.skippedPreferences: SharedPreferences
        get() = getSharedPreferences(SKIPPED_FILE, Context.MODE_PRIVATE)

    private val Context.activeProfileId: Long
        get() = appGraph.activeProfileProvider.activeProfileId
}

/** What asking for a library update did. */
sealed interface LibraryUpdateStart {
    data object Started : LibraryUpdateStart

    /** Starts once the update of the profile named [aheadProfileName], the last one queued before it, finishes. */
    data class Queued(val aheadProfileName: String) : LibraryUpdateStart

    /** The profile already has an update running or waiting, so nothing was queued. */
    data object AlreadyQueued : LibraryUpdateStart
}
