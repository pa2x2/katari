package eu.kanade.tachiyomi.data.backup.storage

import okio.Sink
import okio.Source
import tachiyomi.domain.storage.DocumentUri

/**
 * Backup files, read and written through the [DocumentUri] the document pickers return: on Android through the content
 * resolver, on desktop as plain files.
 */
interface BackupDocuments {
    /** The document's content from its start. Throws when it can't be opened. */
    fun read(uri: DocumentUri): Source

    /** Whether [uri] is a file a backup can be written into, rather than nothing or a folder. */
    fun isWritableFile(uri: DocumentUri): Boolean

    /** Writes the document from its start, replacing whatever it held. */
    fun write(uri: DocumentUri): Sink

    fun delete(uri: DocumentUri)
}
