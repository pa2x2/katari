package app.katari.sync.client.file.transfer

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.file.local.OwnedFiles
import app.katari.sync.client.file.local.contentOf
import app.katari.sync.client.file.local.fileCategoryOf
import app.katari.sync.client.file.local.stored
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.server.SyncServerClient
import app.katari.sync.protocol.file.FileReference
import dev.zacsweers.metro.Inject
import tachiyomi.data.DatabaseHandler
import java.io.File

/**
 * Gets this device's custom covers and fonts to the server before the records naming them (D016, D022), so the server
 * never holds a record whose file it lacks, and the file never waits there unnamed long enough to be removed.
 *
 * The server is asked first ([SyncServerClient.claimFiles]), which also keeps every file asked about for a day, past
 * the push that follows. A file it has goes no further; one it lacks is uploaded from whichever file in place here
 * holds it, when files may move ([FileTransferRule]). One that must wait for Wi-Fi stays queued in `sync_file_uploads`,
 * and only the records naming it wait with it. An upload cut off midway leaves nothing on the server, which removes
 * what it didn't receive whole, so the next sync uploads it again from the start.
 */
@Inject
class FileUploads(
    private val handler: DatabaseHandler,
    private val files: OwnedFiles,
    private val rule: FileTransferRule,
) {
    /**
     * Makes sure the server has [references], and returns those it lacks that this device must still upload. A file the
     * server lacks that no file here holds any more is not waited for: the record naming it was changed since, or the
     * file was lost with a server everywhere.
     */
    suspend fun ensure(client: SyncServerClient, references: Collection<FileReference>): Set<FileReference> {
        val asked = references.distinct()
        if (asked.isEmpty()) return emptySet()
        val missing = asked.chunked(CLAIM_CHUNK).flatMapTo(HashSet()) { client.claimFiles(it).missing }
        asked.filterNot(missing::contains).forEach { uploaded(it) }
        val waiting = HashSet<FileReference>()
        missing.forEach { reference ->
            val source = heldHere(reference)
            when {
                source == null -> uploaded(reference)
                !rule.allowsNow() -> waiting += queue(reference)
                client.uploadFile(reference, source) -> uploaded(reference)
                // The bytes changed while they were read; capture records the change, and the next sync looks again.
                else -> waiting += queue(reference)
            }
        }
        return waiting
    }

    /** Offers every queued file, such as the files a full resync found the server may have lost. */
    suspend fun offerQueued(client: SyncServerClient) {
        val queued = handler.await { sync_filesQueries.uploads(Long.MAX_VALUE).awaitAsList() }
        queued.map { FileReference(fileCategoryOf(it.category), it.sha256, it.size) }
            .chunked(CLAIM_CHUNK)
            .forEach { ensure(client, it) }
    }

    /** A file in place here that holds [reference] now, checked by its content. */
    private suspend fun heldHere(reference: FileReference): File? {
        val owners = handler.await {
            sync_filesQueries.placedAs(reference.category.stored, reference.sha256).awaitAsList()
        }
        return owners.firstNotNullOfOrNull { row ->
            files.locked {
                file(FileOwner.of(reference.category, LocalRef(row.owner)))
                    ?.takeIf { contentOf(it).sha256 == reference.sha256 }
            }
        }
    }

    private suspend fun queue(reference: FileReference): FileReference {
        handler.await { sync_filesQueries.queueUpload(reference.category.stored, reference.sha256, reference.size) }
        return reference
    }

    private suspend fun uploaded(reference: FileReference) {
        handler.await { sync_filesQueries.uploaded(reference.category.stored, reference.sha256) }
    }

    private companion object {
        // Well under the server's request limit, at about 100 bytes a reference.
        const val CLAIM_CHUNK = 500
    }
}
