package tachiyomi.core.common.storage

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.DirectoryIteratorException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes

/** A plain file, which behaves as UniFile's plain files do on Android unless a member says otherwise. */
class PathStorageFile(val path: Path) : StorageFile {
    override val name: String?
        get() = path.fileName?.toString()

    override val uri: String
        get() = path.toUri().toString()

    override val displayablePath: String
        get() = path.toString()

    override val isDirectory: Boolean
        get() = Files.isDirectory(path)

    override val isFile: Boolean
        get() = Files.isRegularFile(path)

    override fun exists(): Boolean = Files.exists(path)

    override fun length(): Long = try {
        if (Files.isRegularFile(path)) Files.size(path) else 0L
    } catch (_: IOException) {
        0L
    }

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

    override fun listWithDetails(): List<ListedStorageFile> {
        if (!isDirectory) return emptyList()
        return try {
            Files.newDirectoryStream(path).use { children -> children.map(::listed) }
        } catch (_: IOException) {
            emptyList()
        } catch (_: DirectoryIteratorException) {
            emptyList()
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

    // As UniFile's plain files make folders, missing parents of a nested name included.
    override fun createDirectory(name: String): StorageFile? {
        if (name.isEmpty()) return null
        val target = child(name)
        if (Files.isDirectory(target)) return PathStorageFile(target)
        if (Files.exists(target)) return null
        return try {
            PathStorageFile(Files.createDirectories(target))
        } catch (_: IOException) {
            null
        }
    }

    // Fails where the name is taken rather than replacing what has it.
    override fun renameTo(name: String): StorageFile? {
        if (name.isEmpty()) return null
        val parent = path.toAbsolutePath().parent ?: return null
        return try {
            PathStorageFile(Files.move(path, File(parent.toFile(), name).toPath()))
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

    override fun availableBytes(): Long = try {
        Files.getFileStore(path).usableSpace
    } catch (_: IOException) {
        -1L
    }

    override fun openInputStream(): InputStream = Files.newInputStream(path)

    override fun openOutputStream(append: Boolean): OutputStream = if (append) {
        Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    } else {
        Files.newOutputStream(path)
    }

    private fun listed(child: Path): ListedStorageFile {
        val attributes = try {
            Files.readAttributes(child, BasicFileAttributes::class.java)
        } catch (_: IOException) {
            null
        }
        val isDirectory = attributes?.isDirectory ?: Files.isDirectory(child)
        return ListedStorageFile(
            file = PathStorageFile(child),
            name = child.fileName?.toString(),
            isDirectory = isDirectory,
            length = if (attributes == null || isDirectory) UNKNOWN else attributes.size(),
            lastModified = attributes?.lastModifiedTime()?.toMillis() ?: UNKNOWN,
        )
    }

    // As java.io.File joins them, which UniFile's plain files use: a name starting with a slash stays inside this
    // folder, where Path.resolve would take it as an absolute path.
    private fun child(name: String): Path = File(path.toFile(), name).toPath()

    private companion object {
        const val UNKNOWN = -1L
    }
}

/** The file behind a storage file, for desktop code that reads files itself. Every storage file on desktop is one. */
fun StorageFile.toPath(): Path = (this as PathStorageFile).path
