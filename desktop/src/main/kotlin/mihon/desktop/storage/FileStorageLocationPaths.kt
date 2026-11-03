package mihon.desktop.storage

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.data.StorageLocationPaths
import tachiyomi.domain.storage.DocumentUri
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Path

/** Storage locations are plain folders, kept as the `file://` URIs the desktop folder picker returns. */
@Inject
class FileStorageLocationPaths : StorageLocationPaths {
    override fun locationOf(folder: DocumentUri): String = folder.value

    override fun displayablePath(location: String): String? = location.toFolderPath()?.toString()
}

/** The folder a `file://` storage location names; null for a value that names none. */
internal fun String.toFolderPath(): Path? = try {
    Path.of(URI(this))
} catch (_: URISyntaxException) {
    null
} catch (_: IllegalArgumentException) {
    null
} catch (_: FileSystemNotFoundException) {
    null
}
