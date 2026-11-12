package app.katari.sync.client.file.capture

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.file.local.OwnedFiles
import app.katari.sync.client.file.local.dataGroup
import app.katari.sync.client.file.local.stored
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.font.ImportedFontKind
import app.katari.sync.client.payload.SyncedFile
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.record.LocalRef
import app.katari.sync.protocol.file.FileCategory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.source.identity.SourceIdScheme
import kotlin.time.Clock

/**
 * Records changes to custom covers and imported fonts as edits of the records that name them, while sync is on, as
 * the capture triggers do for rows: the database never sees these files. It listens to the app's file store itself
 * ([app.katari.sync.client.file.local.LocalFileStore.watch]), so a change made from any screen is heard; with sync off
 * nothing listens.
 *
 * A change is told apart by content: the file now in place is compared with what sync last saw there and with what
 * the owner's record names. A file sync put there itself, or a change back to what the record names, is no edit; any
 * other content is the person's, recorded with the time it was heard and queued for upload. Local-source entries'
 * covers never sync (D016).
 *
 * Files of a kind this device doesn't sync ([FileCategory.dataGroup]) are left alone, neither recorded nor uploaded;
 * [rescan] records what changed among them meanwhile once the device syncs them again.
 */
@Inject
@SingleIn(AppScope::class)
class FileCapture(
    private val handler: DatabaseHandler,
    private val files: OwnedFiles,
    private val preferences: SyncPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Running? = null

    /** Starts listening, unless it already does. Changes are heard from the moment this returns. */
    fun start() {
        synchronized(this) { if (running == null) running = listen() }
    }

    private fun listen(): Running {
        val heard = Channel<Heard>(Channel.UNLIMITED)
        val watch = files.watch { owner -> heard.trySend(Change(owner)) }
        val recording = scope.launch {
            for (next in heard) {
                when (next) {
                    is Change -> record(next)
                    is Flush -> next.done.complete(Unit)
                }
            }
        }
        return Running(watch, heard, recording)
    }

    /** Returns once every change heard so far is recorded, so a drain after it sees them; at once when not listening. */
    suspend fun flush() {
        val flush = Flush()
        val queued = synchronized(this) { running?.heard?.trySend(flush)?.isSuccess == true }
        if (queued) flush.done.await()
    }

    /** Stops listening; changes heard but not recorded yet are dropped, as sync is off. */
    fun stop() {
        val stopped = synchronized(this) { running.also { running = null } } ?: return
        stopped.watch.close()
        stopped.recording.cancel()
    }

    /**
     * Records what changed in [category]'s files while this device didn't sync them, as changes made now: every owner
     * with a file here, and every owner sync knew the file of. A file still as sync last placed it is no change, and
     * takes what its record names now.
     */
    suspend fun rescan(category: FileCategory) {
        val known = handler.await { sync_filesQueries.ownersOf(category.stored).awaitAsList() }
            .map { FileOwner.of(category, LocalRef(it)) }
        val present = when (category) {
            FileCategory.COVER -> handler.await { sync_filesQueries.syncedEntryIds().awaitAsList() }
                .map { FileOwner.CustomCover(it) }
            FileCategory.FONT -> handler.await { sync_local_rowsQueries.profiles().awaitAsList() }
                .flatMap { profile -> files.locked { fonts(profile._id) }.map { FileOwner.Font(profile._id, it) } }
        }
        (known + present).distinct().forEach { record(Change(it)) }
    }

    private suspend fun record(change: Change) {
        try {
            files.locked {
                val now = content(change.owner)
                handler.await(inTransaction = true) { record(change, now) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Couldn't record a change of ${change.owner}" }
        }
    }

    private suspend fun Database.record(change: Change, now: SyncedFile?) {
        if (!sync_stateQueries.get().awaitAsOne().enabled) return
        val owner = change.owner
        val category = owner.category.stored
        val ref = owner.ref.json
        val row = sync_filesQueries.get(category, ref).awaitAsOneOrNull()
        if (!ownerSyncs(owner)) {
            // The entry is gone, or never syncs; its record keeps the cover it names (D013, KEEP). Or the profile is
            // gone, whose deletion stands for its fonts', as for its other rows (CaptureTrigger.owners)
            if (row != null) sync_filesQueries.delete(category, ref)
            return
        }
        if (!preferences.choices().syncs(owner.category.dataGroup)) return
        when {
            row == null && now == null -> return
            row != null && now?.sha256 == row.placed_sha256 -> return
            row != null && now?.sha256 == row.wanted_sha256 -> {
                sync_filesQueries.setPlaced(sha256 = now?.sha256, category = category, owner = ref)
                return
            }
        }
        sync_filesQueries.holds(category = category, owner = ref, sha256 = now?.sha256, size = now?.size ?: 0)
        if (now != null) sync_filesQueries.queueUpload(category, now.sha256, now.size)
        val (kind, fields) = when (owner) {
            is FileOwner.CustomCover -> EntryKind.name to EntryKind.CUSTOM_COVER_FIELDS
            is FileOwner.Font -> ImportedFontKind.name to EVERY_FIELD
        }
        sync_outboxQueries.recordFieldEdit(kind = kind, localRef = ref, fields = fields, changedAt = change.heardAt)
    }

    private suspend fun Database.ownerSyncs(owner: FileOwner): Boolean = when (owner) {
        is FileOwner.CustomCover -> sync_local_rowsQueries.entryRow(owner.entryId).awaitAsOneOrNull()
            ?.let { it.id_scheme != SourceIdScheme.LOCAL.key } == true
        is FileOwner.Font -> sync_local_rowsQueries.profileUuid(owner.profileId).awaitAsOneOrNull() != null
    }

    private sealed interface Heard

    private class Change(val owner: FileOwner) : Heard {
        val heardAt: Long = Clock.System.now().toEpochMilliseconds()
    }

    private class Flush : Heard {
        val done = CompletableDeferred<Unit>()
    }

    private class Running(val watch: AutoCloseable, val heard: Channel<Heard>, val recording: Job)

    private companion object {
        const val EVERY_FIELD = -1L
    }
}
