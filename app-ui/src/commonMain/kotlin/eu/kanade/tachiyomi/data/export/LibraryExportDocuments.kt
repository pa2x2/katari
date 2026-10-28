package eu.kanade.tachiyomi.data.export

import tachiyomi.domain.storage.DocumentUri

/** The document the user picked to export the library list into. */
interface LibraryExportDocuments {
    /** Replaces [document]'s content with [text]; does nothing where the document can't be opened. */
    fun write(document: DocumentUri, text: String)
}
