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
import eu.kanade.tachiyomi.data.library.queue.ChainedLibraryUpdate
import eu.kanade.tachiyomi.data.library.queue.LibraryUpdateWorkChain
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.guava.await
import logcat.LogPriority
import tachiyomi.core.common.preference.SourcePreferenceProfile
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.update.model.LibraryUpdateProgress
import tachiyomi.domain.library.update.model.LibraryUpdateScope
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The [LibraryUpdateWorkChain] as one WorkManager chain of [LibraryUpdateJob]s.
 *
 * WorkManager cancels everything queued behind a work that is cancelled, so nothing here cancels through it: a running
 * update is stopped from inside its worker, and a waiting one is marked to skip itself when its turn comes.
 */
@Inject
@SingleIn(AppScope::class)
class AndroidLibraryUpdateWorkChain(
    private val application: Application,
    private val activeProfileProvider: ActiveProfileProvider,
    private val selectionFile: LibraryUpdateSelectionFile,
) : LibraryUpdateWorkChain {
    /** Updates running in this process, by work id, so a stop can end one without cancelling the chain. */
    private val running = ConcurrentHashMap<UUID, Job>()

    private val skippedPreferences: SharedPreferences
        get() = application.getSharedPreferences(SKIPPED_FILE, Context.MODE_PRIVATE)

    override val updates: Flow<List<ChainedLibraryUpdate>> =
        combine(application.workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_NAME), skippedFlow()) { infos, skipped ->
            infos.pending(skipped)
        }

    /** Updates running or waiting; one taken out of the queue only waits to end, so it isn't one of them. */
    override suspend fun currentUpdates(): List<ChainedLibraryUpdate> {
        val skipped = skippedPreferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        return application.workManager.getWorkInfosForUniqueWork(UNIQUE_NAME).await().pending(skipped)
    }

    override suspend fun append(input: LibraryUpdateInput) {
        val request = OneTimeWorkRequestBuilder<LibraryUpdateJob>()
            .addTag(TAG)
            .addTag("$PROFILE_TAG_PREFIX${input.profileId}")
            .addTag("$QUEUED_AT_TAG_PREFIX${System.currentTimeMillis()}")
            .apply { if (input.automatic) addTag(TAG_AUTOMATIC) }
            .setInputData(input.toData())
            .build()
        application.workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            .result.await()
    }

    override suspend fun remove(update: ChainedLibraryUpdate) {
        val id = UUID.fromString(update.id)
        val runningHere = running[id]?.takeIf { update.isRunning }
        if (runningHere != null) runningHere.cancel() else markSkipped(id)
    }

    /**
     * Runs [update] as the chained work [id] with [inputData] and [tags], unless it was taken out of the chain while it
     * waited. It ends as finished either way, stopped or not, so the updates queued behind it go ahead.
     */
    internal suspend fun runChained(
        id: UUID,
        inputData: Data,
        tags: Set<String>,
        update: suspend (LibraryUpdateInput) -> Unit,
    ) {
        val input = inputOf(inputData, tags)
        coroutineScope {
            // Extensions read logins and API keys from this profile's source preferences, not the active profile's.
            val job = async(SourcePreferenceProfile.of(input.profileId), start = CoroutineStart.LAZY) { update(input) }
            // Registered before the skip check, so a stop either finds the update running or marks it skipped first.
            running[id] = job
            try {
                if (consumeSkip(id)) {
                    logcat(LogPriority.INFO) { "Skipping library update that was taken out of the queue" }
                    // Never started, it would keep the scope from completing.
                    job.cancel()
                    input.selectionName?.let(selectionFile::delete)
                } else {
                    job.await()
                }
            } catch (_: CancellationException) {
                // Stopped by the user, or by WorkManager; either way the updates queued behind it go ahead.
                logcat(LogPriority.INFO) { "Library update cancelled" }
            } finally {
                running.remove(id)
            }
        }
    }

    /** The profile the update with [inputData] runs for. */
    internal fun profileIdOf(inputData: Data): Long = inputOf(inputData, emptySet()).profileId

    private fun inputOf(data: Data, tags: Set<String>) = LibraryUpdateInput(
        profileId = data.getLong(KEY_PROFILE, -1L)
            .takeIf { it >= 0L }
            // Requests queued by an older version carry no profile.
            ?: activeProfileProvider.activeProfileId,
        automatic = TAG_AUTOMATIC in tags,
        categoryId = data.getLong(KEY_CATEGORY, -1L).takeIf { it != -1L },
        sourceId = data.getLong(KEY_SOURCE, -1L).takeIf { it != -1L },
        entryType = data.getString(KEY_ENTRY_TYPE)?.let { serialized ->
            EntryType.entries.find { it.name == serialized }
        },
        selectionName = data.getString(KEY_SELECTION),
        skippedOfLatest = data.getBoolean(KEY_SKIPPED_OF_LATEST, false),
    )

    private fun LibraryUpdateInput.toData(): Data = Data.Builder()
        .apply {
            categoryId?.let { putLong(KEY_CATEGORY, it) }
            sourceId?.let { putLong(KEY_SOURCE, it) }
            entryType?.let { putString(KEY_ENTRY_TYPE, it.name) }
            selectionName?.let { putString(KEY_SELECTION, it) }
            if (skippedOfLatest) putBoolean(KEY_SKIPPED_OF_LATEST, true)
        }
        .putLong(KEY_PROFILE, profileId)
        .build()

    private fun List<WorkInfo>.pending(skipped: Set<String>): List<ChainedLibraryUpdate> {
        return filter { !it.state.isFinished && it.id.toString() !in skipped }
            .map { info ->
                ChainedLibraryUpdate(
                    id = info.id.toString(),
                    profileId = info.tagValue(PROFILE_TAG_PREFIX)?.toLongOrNull(),
                    // WorkInfo doesn't tell the order of a chain.
                    queuedAt = info.tagValue(QUEUED_AT_TAG_PREFIX)?.toLongOrNull() ?: Long.MAX_VALUE,
                    isRunning = info.state == WorkInfo.State.RUNNING,
                    progress = info.progress.toLibraryUpdateProgress(),
                )
            }
    }

    private fun WorkInfo.tagValue(prefix: String): String? {
        return tags.firstNotNullOfOrNull { tag -> tag.removePrefix(prefix).takeIf { it != tag } }
    }

    /** True once for an update [remove] took out of the queue, which then ends without running. */
    @Synchronized
    private fun consumeSkip(id: UUID): Boolean {
        val preferences = skippedPreferences
        val skipped = preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        if (id.toString() !in skipped) return false
        preferences.edit { putStringSet(SKIPPED_KEY, skipped - id.toString()) }
        return true
    }

    @Synchronized
    private fun markSkipped(id: UUID) {
        val preferences = skippedPreferences
        val skipped = preferences.getStringSet(SKIPPED_KEY, emptySet()).orEmpty()
        preferences.edit { putStringSet(SKIPPED_KEY, skipped + id.toString()) }
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

    /** [progress] as the running work publishes it, so [updates] can read it back from its [WorkInfo]. */
    internal fun progressData(progress: LibraryUpdateProgress): Data = workDataOf(
        KEY_PROGRESS_SCOPE to progress.scope.name,
        KEY_PROGRESS_SCOPE_NAME to progress.scopeName,
        KEY_PROGRESS_COMPLETED to progress.completed,
        KEY_PROGRESS_TOTAL to progress.total,
        KEY_PROGRESS_SKIPPED to progress.skipped,
        KEY_PROGRESS_STARTED_AT to progress.startedAt,
        KEY_PROGRESS_CHECKING to progress.checking.toTypedArray(),
    )

    private fun Data.toLibraryUpdateProgress(): LibraryUpdateProgress? {
        val scope = getString(KEY_PROGRESS_SCOPE)
            ?.let { name -> LibraryUpdateScope.entries.find { it.name == name } }
            ?: return null
        return LibraryUpdateProgress(
            scope = scope,
            scopeName = getString(KEY_PROGRESS_SCOPE_NAME),
            completed = getInt(KEY_PROGRESS_COMPLETED, 0),
            total = getInt(KEY_PROGRESS_TOTAL, 0),
            skipped = getInt(KEY_PROGRESS_SKIPPED, 0),
            startedAt = getLong(KEY_PROGRESS_STARTED_AT, 0L),
            checking = getNullableStringArray(KEY_PROGRESS_CHECKING)?.filterNotNull().orEmpty(),
        )
    }

    private companion object {
        const val UNIQUE_NAME = "LibraryUpdate-queue"
        const val TAG = "LibraryUpdate"

        /** Marks updates the automatic schedule queued. */
        const val TAG_AUTOMATIC = "LibraryUpdate-auto"
        const val PROFILE_TAG_PREFIX = "LibraryUpdate-profile-"

        /** Followed by when the update was queued, as WorkInfo doesn't tell the order of a chain. */
        const val QUEUED_AT_TAG_PREFIX = "LibraryUpdate-queued-"

        const val SKIPPED_FILE = "library_update_queue"
        const val SKIPPED_KEY = "skipped"

        /** Key for category to update. */
        const val KEY_CATEGORY = "category"

        /** Key for source to update. */
        const val KEY_SOURCE = "source"

        /** Key for entry type to update. */
        const val KEY_ENTRY_TYPE = "entry_type"

        /** Name of the [LibraryUpdateSelectionFile] listing the entries to check. */
        const val KEY_SELECTION = "selection"

        /** Checks the entries the latest update skipped by rules. */
        const val KEY_SKIPPED_OF_LATEST = "skipped_of_latest"

        /** Id of the profile the update runs for. */
        const val KEY_PROFILE = "profile"

        const val KEY_PROGRESS_SCOPE = "progress_scope"
        const val KEY_PROGRESS_SCOPE_NAME = "progress_scope_name"
        const val KEY_PROGRESS_COMPLETED = "progress_completed"
        const val KEY_PROGRESS_TOTAL = "progress_total"
        const val KEY_PROGRESS_SKIPPED = "progress_skipped"
        const val KEY_PROGRESS_STARTED_AT = "progress_started_at"
        const val KEY_PROGRESS_CHECKING = "progress_checking"
    }
}
