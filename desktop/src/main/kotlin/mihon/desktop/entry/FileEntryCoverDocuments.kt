package mihon.desktop.entry

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverDocuments
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.storage.DocumentUri
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads the plain file the desktop open dialog returns a `file://` URI of. Desktop has no local source, whose entries'
 * folders a restored backup doesn't bring along, so local entries' covers can't be edited.
 */
@Inject
class FileEntryCoverDocuments : EntryCoverDocuments {
    override val editsLocalCovers: Boolean = false

    override fun open(document: DocumentUri): InputStream? =
        runCatching { Files.newInputStream(Path.of(URI(document.value))) }.getOrNull()

    override fun replaceLocalCover(entry: Entry, image: InputStream) {
        error("Local entries' covers can't be edited on desktop")
    }
}
