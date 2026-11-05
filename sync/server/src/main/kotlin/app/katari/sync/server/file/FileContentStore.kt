package app.katari.sync.server.file

import app.katari.sync.protocol.file.FileCategory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists

/**
 * File contents in the data directory: `files/<account id>/<covers|fonts>/<first two hex digits>/<sha256>`, named by
 * content so a file two devices upload is stored once, and `uploads/` for files still arriving. Nothing in `uploads/`
 * is complete once the server stops, so [prepare] empties it.
 */
class FileContentStore(dataDirectory: Path) {
    private val files = dataDirectory.resolve("files")
    private val uploads = dataDirectory.resolve("uploads")

    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    fun prepare() {
        files.createDirectories()
        uploads.deleteRecursively()
        uploads.createDirectories()
    }

    fun pathOf(file: StoredFile): Path = files
        .resolve(file.accountId.toString())
        .resolve(file.category.folder)
        .resolve(file.sha256.take(2))
        .resolve(file.sha256)

    fun exists(file: StoredFile): Boolean = pathOf(file).exists()

    fun newUpload(): Path = Files.createTempFile(uploads, "upload-", "")

    fun moveIntoPlace(upload: Path, file: StoredFile) {
        val target = pathOf(file)
        target.parent.createDirectories()
        Files.move(upload, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    fun delete(file: StoredFile) {
        pathOf(file).deleteIfExists()
    }

    private val FileCategory.folder: String
        get() = when (this) {
            FileCategory.COVER -> "covers"
            FileCategory.FONT -> "fonts"
        }
}
