package mihon.desktop.export

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.export.LibraryExportDocuments
import tachiyomi.domain.storage.DocumentUri
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/** The library list as the plain file the desktop save dialog returns a `file://` URI of, created if it is new. */
@Inject
class FileLibraryExportDocuments : LibraryExportDocuments {
    override fun write(document: DocumentUri, text: String) {
        Files.write(Path.of(URI(document.value)), text.toByteArray())
    }
}
