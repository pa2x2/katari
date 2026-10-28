package mihon.desktop.backup

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.storage.BackupDocuments
import okio.Sink
import okio.Source
import okio.sink
import okio.source
import tachiyomi.domain.storage.DocumentUri
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Backups as the plain files the desktop document pickers return `file://` URIs of. The file a backup is saved as
 * needn't exist yet; it is created when the backup is written.
 */
@Inject
class FileBackupDocuments : BackupDocuments {
    override fun read(uri: DocumentUri): Source = path(uri).source()

    override fun isWritableFile(uri: DocumentUri): Boolean = !Files.isDirectory(path(uri))

    override fun write(uri: DocumentUri): Sink = path(uri).sink(
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE,
    )

    override fun delete(uri: DocumentUri) {
        Files.deleteIfExists(path(uri))
    }

    private fun path(uri: DocumentUri): Path = Path.of(URI(uri.value))
}
