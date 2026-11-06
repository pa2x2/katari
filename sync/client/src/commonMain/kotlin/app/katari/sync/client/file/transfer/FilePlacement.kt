package app.katari.sync.client.file.transfer

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.file.local.OwnedFiles
import app.katari.sync.client.file.local.contentOf
import app.katari.sync.client.file.local.fileCategoryOf
import app.katari.sync.client.file.local.stored
import app.katari.sync.client.payload.SyncedFile
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.data.Sync_local_files
import app.katari.sync.protocol.file.FileReference
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.core.platform.storage.AppPrivateDirectories
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import java.io.File
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Brings each custom cover and font in place to what its record names, after the records are applied: a file another
 * owner here already holds is copied, any other is downloaded, checked against its hash and then put in place, and a
 * file no longer named is removed. Records never wait for this; until its file is here an entry shows the source's
 * cover and a font isn't in the list.
 *
 * A file is replaced or removed only while it is still what sync last saw in place ([OwnedFiles]), so a change the
 * person made meanwhile wins and is captured as theirs. Downloads follow the Wi-Fi rule ([FileTransferRule]). A file
 * the server says it doesn't have, or whose bytes don't match their name, is reported
 * ([app.katari.sync.client.status.SyncStatus.Files.missingOnServer]) and asked for again only a day later or after a
 * full resync, never on every sync.
 */
@Inject
class FilePlacement(
    private val handler: DatabaseHandler,
    private val files: OwnedFiles,
    private val rule: FileTransferRule,
    private val directories: AppPrivateDirectories,
) {
    suspend fun place(client: SyncServerClient) {
        val unsettled = handler.await { sync_filesQueries.unsettled().awaitAsList() }
        val (removals, wanted) = unsettled.partition { it.wanted_sha256 == null }
        // Copies first, so a file whose owner gives it up, as an entry moved to another profile does, is still there to
        // copy from.
        wanted.groupBy { FileReference(fileCategoryOf(it.category), it.wanted_sha256!!, it.wanted_size) }
            .forEach { (reference, owners) -> place(client, reference, owners) }
        removals.forEach { removeUnnamed(it) }
    }

    private suspend fun place(client: SyncServerClient, reference: FileReference, rows: List<Sync_local_files>) {
        val owners = rows.map { FileOwner.of(reference.category, LocalRef(it.owner)) }
        val copied = files.locked {
            val source = heldHere(reference) ?: return@locked false
            owners.forEach { owner -> placeIfUnchanged(owner, reference, source) }
            true
        }
        if (copied) return
        val now = Clock.System.now().toEpochMilliseconds()
        if (rows.all { it.unavailable_since?.let { since -> now - since < RETRY_UNAVAILABLE } == true }) return
        if (!rule.allowsNow()) return
        val download = withContext(Dispatchers.IO) { File.createTempFile("download", ".part", staging()) }
        try {
            val arrived = client.downloadFile(reference, download)
            val intact = arrived && withContext(Dispatchers.IO) { contentOf(download) } == reference.synced
            if (!intact) {
                logcat(LogPriority.WARN) {
                    "Synced file ${reference.sha256} ${if (arrived) "doesn't match its name" else "isn't on the server"}"
                }
                handler.await { sync_filesQueries.markUnavailable(now, reference.category.stored, reference.sha256) }
                return
            }
            files.locked { owners.forEach { owner -> placeIfUnchanged(owner, reference, download) } }
        } finally {
            withContext(Dispatchers.IO) { download.delete() }
        }
    }

    /** A file in place here that holds [reference] now, checked by its content. */
    private suspend fun OwnedFiles.Locked.heldHere(reference: FileReference): File? {
        val holders = handler.await {
            sync_filesQueries.placedAs(reference.category.stored, reference.sha256).awaitAsList()
        }
        return holders.firstNotNullOfOrNull { row ->
            file(FileOwner.of(reference.category, LocalRef(row.owner)))
                ?.takeIf { contentOf(it).sha256 == reference.sha256 }
        }
    }

    private suspend fun OwnedFiles.Locked.placeIfUnchanged(owner: FileOwner, reference: FileReference, source: File) {
        val row = current(owner) ?: return
        if (row.wanted_sha256 != reference.sha256) return
        if (content(owner)?.sha256 != row.placed_sha256) return
        put(owner, source)
        handler.await { sync_filesQueries.setPlaced(reference.sha256, row.category, row.owner) }
    }

    /**
     * Removes a file no record names any more. Its owner may be gone already, as the old entry of a move to another
     * profile is: the file is still removed, since custom covers are kept by entry ID and nothing else would.
     */
    private suspend fun removeUnnamed(row: Sync_local_files) {
        val owner = FileOwner.of(fileCategoryOf(row.category), LocalRef(row.owner))
        files.locked {
            val known = handler.await { sync_filesQueries.get(row.category, row.owner).awaitAsOneOrNull() }
            if (known == null || known.wanted_sha256 != null) return@locked
            val inPlace = content(owner)?.sha256
            val unchanged = inPlace == known.placed_sha256
            if (unchanged && inPlace != null) remove(owner)
            if (current(owner) != null && unchanged) {
                handler.await { sync_filesQueries.setPlaced(null, known.category, known.owner) }
            }
        }
    }

    /**
     * The owner's row, or null once the owner is gone: an entry or profile deleted since its record named the file,
     * whose row is dropped.
     */
    private suspend fun current(owner: FileOwner): Sync_local_files? = handler.await {
        val exists = when (owner) {
            is FileOwner.CustomCover -> sync_applyQueries.entryExists(owner.entryId).awaitAsOne()
            is FileOwner.Font -> sync_local_rowsQueries.profileUuid(owner.profileId).awaitAsOneOrNull() != null
        }
        if (!exists) {
            sync_filesQueries.delete(owner.category.stored, owner.ref.json)
            return@await null
        }
        sync_filesQueries.get(owner.category.stored, owner.ref.json).awaitAsOneOrNull()
    }

    private fun staging(): File = File(directories.cache, STAGING).apply { mkdirs() }

    private val FileReference.synced get() = SyncedFile(sha256, size)

    private companion object {
        const val STAGING = "sync-files"
        val RETRY_UNAVAILABLE = 1.days.inWholeMilliseconds
    }
}
