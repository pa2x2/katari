package eu.kanade.tachiyomi.data.backup.storage

import dev.zacsweers.metro.Inject
import okio.Sink
import okio.Source
import okio.sink
import okio.source
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.domain.storage.DocumentUri
import java.io.FileNotFoundException

/** Backup files, read and written through the [DocumentUri] the document pickers return. */
@Inject
class BackupDocuments(private val files: StorageFileResolver) {
    /** The document's content from its start. Throws when it can't be opened. */
    fun read(uri: DocumentUri): Source = file(uri).openInputStream().source()

    /**
     * Whether [uri] is a file a backup can be written into, rather than a folder or nothing at all. Desktop save
     * pickers return files that don't exist yet, which writing the backup creates.
     */
    fun isWritableFile(uri: DocumentUri): Boolean = files.fromUri(uri.value)?.let { it.isFile || !it.exists() } == true

    /** Writes the document from its start, replacing whatever it held. */
    fun write(uri: DocumentUri): Sink = file(uri).openOutputStream().sink()

    fun delete(uri: DocumentUri) {
        files.fromUri(uri.value)?.delete()
    }

    private fun file(uri: DocumentUri): StorageFile =
        files.fromUri(uri.value) ?: throw FileNotFoundException("No storage opens the backup document ${uri.value}")
}
