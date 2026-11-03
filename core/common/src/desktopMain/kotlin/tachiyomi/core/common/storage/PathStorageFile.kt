package tachiyomi.core.common.storage

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.DirectoryIteratorException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

/** A plain file, which behaves as UniFile's plain files do on Android. */
class PathStorageFile(val path: Path) : StorageFile {
    override val name: String?
        get() = path.fileName?.toString()

    override val uri: String
        get() = path.toUri().toString()

    override val isDirectory: Boolean
        get() = Files.isDirectory(path)

    override val isFile: Boolean
        get() = Files.isRegularFile(path)

    override fun lastModified(): Long = try {
        Files.getLastModifiedTime(path).toMillis()
    } catch (_: IOException) {
        0L
    }

    override fun listFiles(): List<StorageFile>? {
        if (!isDirectory) return null
        return try {
            Files.newDirectoryStream(path).use { children -> children.map(::PathStorageFile) }
        } catch (_: IOException) {
            null
        } catch (_: DirectoryIteratorException) {
            null
        }
    }

    override fun findFile(name: String): StorageFile? {
        if (name.isEmpty()) return null
        return child(name).takeIf { Files.exists(it) }?.let(::PathStorageFile)
    }

    override fun createFile(name: String): StorageFile? {
        if (name.isEmpty()) return null
        val target = child(name)
        if (Files.exists(target)) return if (Files.isRegularFile(target)) PathStorageFile(target) else null
        return try {
            PathStorageFile(Files.createFile(target))
        } catch (_: IOException) {
            null
        }
    }

    // Links are deleted, not followed, so deleting a folder never reaches into another one.
    override fun delete(): Boolean = try {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            Files.walk(path).use { files -> files.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
            true
        } else {
            Files.deleteIfExists(path)
        }
    } catch (_: IOException) {
        false
    }

    override fun openInputStream(): InputStream = Files.newInputStream(path)

    override fun openOutputStream(): OutputStream = Files.newOutputStream(path)

    // As java.io.File joins them, which UniFile's plain files use: a name starting with a slash stays inside this
    // folder, where Path.resolve would take it as an absolute path.
    private fun child(name: String): Path = File(path.toFile(), name).toPath()
}

/** The file behind a storage file, for desktop code that reads files itself. Every storage file on desktop is one. */
fun StorageFile.toPath(): Path = (this as PathStorageFile).path
