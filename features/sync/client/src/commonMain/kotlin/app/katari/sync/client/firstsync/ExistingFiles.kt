package app.katari.sync.client.firstsync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.file.local.OwnedFiles
import app.katari.sync.client.file.local.stored
import app.katari.sync.client.kind.entry.EntryKind
import app.katari.sync.client.kind.font.ImportedFontKind
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.payload.SyncedFile
import app.katari.sync.client.preference.SyncPreferences
import dev.zacsweers.metro.Inject
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler

/**
 * Records the custom covers and fonts a device held before it joined an account, alongside its rows ([ExistingRows]):
 * each is named as the file in place for its owner, queued for upload, and its record goes into the outbox touching no
 * field, so the file fills in what the account lacks and gives way to every cover or font set anywhere since. Local-
 * source entries' covers never sync (D016).
 *
 * Recording twice is harmless, as with rows. Files of a kind this device doesn't sync are left for
 * [app.katari.sync.client.file.capture.FileCapture.rescan] once it does.
 */
@Inject
class ExistingFiles(
    private val handler: DatabaseHandler,
    private val files: OwnedFiles,
    private val preferences: SyncPreferences,
) {
    suspend fun record() {
        val choices = preferences.choices()
        if (choices.syncs(DataGroup.COVERS)) {
            handler.await { sync_filesQueries.syncedEntryIds().awaitAsList() }.forEach { entryId ->
                record(FileOwner.CustomCover(entryId), EntryKind.name)
            }
        }
        if (choices.syncs(DataGroup.FONTS)) {
            handler.await { sync_local_rowsQueries.profiles().awaitAsList() }.forEach { profile ->
                files.locked { fonts(profile._id) }.forEach { fileName ->
                    record(FileOwner.Font(profile._id, fileName), ImportedFontKind.name)
                }
            }
        }
    }

    private suspend fun record(owner: FileOwner, kind: String) {
        files.locked {
            val content = content(owner) ?: return@locked
            handler.await(inTransaction = true) { record(owner, kind, content) }
        }
    }

    private suspend fun Database.record(owner: FileOwner, kind: String, content: SyncedFile) {
        val category = owner.category.stored
        sync_filesQueries.holds(category, owner.ref.json, content.sha256, content.size)
        sync_filesQueries.queueUpload(category, content.sha256, content.size)
        sync_outboxQueries.recordUnstamped(kind, owner.ref.json)
    }
}
