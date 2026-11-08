package app.katari.desktop.entry

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverDocuments
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.source.local.image.LocalCoverManager
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads the plain file the desktop open dialog returns a `file://` URI of; a local entry's cover is written by the local
 * source.
 */
@Inject
class FileEntryCoverDocuments(
    private val localCoverManager: LocalCoverManager,
) : EntryCoverDocuments {
    override val editsLocalCovers: Boolean = true

    override fun open(document: DocumentUri): InputStream? =
        runCatching { Files.newInputStream(Path.of(URI(document.value))) }.getOrNull()

    override fun replaceLocalCover(entry: Entry, image: InputStream) {
        localCoverManager.update(entry.url, image)
    }
}
