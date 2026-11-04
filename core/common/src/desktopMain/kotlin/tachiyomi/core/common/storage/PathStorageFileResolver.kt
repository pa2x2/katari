package tachiyomi.core.common.storage

import java.net.URI
import java.net.URISyntaxException
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Path

/** Desktop storage is plain files, so only `file:` URIs open. */
class PathStorageFileResolver : StorageFileResolver {
    override fun fromUri(uri: String): StorageFile? = try {
        URI(uri).takeIf { it.scheme == "file" }?.let { PathStorageFile(Path.of(it)) }
    } catch (_: URISyntaxException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: FileSystemNotFoundException) {
        null
    }
}
