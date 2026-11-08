package app.katari.sync.client.setting.capture

import app.katari.sync.client.kind.setting.ProfileSettingKind
import app.katari.sync.client.kind.setting.SharedSettingKind
import app.katari.sync.client.kind.setting.SourceSettingKind
import app.katari.sync.client.setting.read.SettingReads
import app.katari.sync.client.setting.scope.PreferenceScope
import app.katari.sync.client.setting.scope.PreferenceScopes
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mihon.feature.profiles.core.PreferenceWrite
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import mihon.feature.profiles.sourcepreferences.SourceSettingWrite
import tachiyomi.data.DatabaseHandler
import kotlin.time.Clock

/**
 * Records edits of person-level preferences and of source settings into the outbox while sync is on, as the capture
 * triggers do for the database, which never sees preferences. It listens to the stores themselves, so every writer is
 * heard; with sync off nothing listens. Each edit carries the time it was made.
 *
 * Writes that are no one's edit never reach it: at process start it starts only once the app's upgrade migrations
 * have run ([app.katari.sync.client.capture.SyncCapture.resume]); a source's settings file from before profiles is moved before it is watched
 * ([SourcePreferenceFiles]); and when restore writes the default profile's preferences into the shared store as
 * well, those keys belong to the profile, which no shared setting does.
 */
@Inject
@SingleIn(AppScope::class)
class SettingsCapture(
    private val handler: DatabaseHandler,
    private val profileStore: ProfileStore,
    private val sourceFiles: SourcePreferenceFiles,
    private val settings: SettingReads,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Running? = null

    /** Starts listening, unless it already does. Edits are heard from the moment this returns. */
    fun start() {
        synchronized(this) { if (running == null) running = listen() }
    }

    private fun listen(): Running {
        val heard = Channel<Heard>(Channel.UNLIMITED)
        val watches = listOf(
            profileStore.watchPreferenceWrites { write -> edit(write)?.let(heard::trySend) },
            sourceFiles.watchWrites { write -> heard.trySend(edit(write)) },
        )
        val recording = scope.launch {
            for (next in heard) {
                when (next) {
                    is Edit -> record(next)
                    is Flush -> next.done.complete(Unit)
                }
            }
        }
        return Running(watches, heard, recording)
    }

    /**
     * Returns once every edit heard so far is in the outbox, so a drain after it sees them; at once when not
     * listening. Edits a store's listeners deliver later, such as Android's on the main thread, are not waited for.
     */
    suspend fun flush() {
        val flush = Flush()
        val queued = synchronized(this) { running?.heard?.trySend(flush)?.isSuccess == true }
        if (queued) flush.done.await()
    }

    /** Stops listening; edits heard but not recorded yet are dropped, as sync is off. */
    fun stop() {
        val stopped = synchronized(this) { running.also { running = null } } ?: return
        stopped.watches.forEach(AutoCloseable::close)
        stopped.recording.cancel()
    }

    private fun edit(write: PreferenceWrite): Edit? {
        if (PreferenceScopes.of(write.key) != PreferenceScope.PERSON) return null
        val profileId = write.profileId
        return when {
            profileId != null -> Edit(ProfileSettingKind.name, ProfileSettingKind.ref(profileId, write.key).json)
            settings.isProfileKey(write.key) -> null
            else -> Edit(SharedSettingKind.name, SharedSettingKind.ref(write.key).json)
        }
    }

    private fun edit(write: SourceSettingWrite): Edit = when (val key = write.key) {
        null -> Edit(SourceSettingKind.name, SourceSettingKind.refPrefix(write.profileId, write.sourceId), all = true)
        else -> Edit(SourceSettingKind.name, SourceSettingKind.ref(write.profileId, write.sourceId, key).json)
    }

    private suspend fun record(edit: Edit) {
        handler.await {
            if (edit.all) {
                sync_outboxQueries.recordEditOfEach(
                    changedAt = edit.changedAt,
                    kind = edit.kind,
                    localRefPrefix = edit.localRef,
                )
            } else {
                sync_outboxQueries.recordEdit(kind = edit.kind, localRef = edit.localRef, changedAt = edit.changedAt)
            }
        }
    }

    private sealed interface Heard

    /** An edit of the record [localRef] locates, or, when [all], of every record whose local ref starts with it. */
    private class Edit(val kind: String, val localRef: String, val all: Boolean = false) : Heard {
        val changedAt: Long = Clock.System.now().toEpochMilliseconds()
    }

    private class Flush : Heard {
        val done = CompletableDeferred<Unit>()
    }

    private class Running(val watches: List<AutoCloseable>, val heard: Channel<Heard>, val recording: Job)
}
