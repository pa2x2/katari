package eu.kanade.tachiyomi.data.export

import dev.zacsweers.metro.Inject
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.domain.storage.DocumentUri

/** The document the user picked to export the library list into. */
@Inject
class LibraryExportDocuments(private val files: StorageFileResolver) {
    /** Replaces [document]'s content with [text], creating it where it is new; does nothing where it can't be opened. */
    fun write(document: DocumentUri, text: String) {
        files.fromUri(document.value)?.openOutputStream()?.use { output -> output.write(text.toByteArray()) }
    }
}
