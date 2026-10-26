package eu.kanade.tachiyomi.extension.file

import android.content.Context
import eu.kanade.tachiyomi.util.lang.Hash
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File

/**
 * Where file extensions live: `files/extensions/<kind>/<install>/`, one install folder per extension, named by a hash of
 * its ID since IDs can hold anything. An install folder holds `manifest.json` and one folder per version, named by its
 * code's SHA-256. An install or update writes the new version's folder first and its manifest last, then removes the
 * other versions, so a crash at any point leaves either the old version or the new one, never a mix.
 */
internal class FileExtensionStorage(context: Context) {

    private val root = File(context.filesDir, "extensions")

    fun installDir(kind: FileExtensionKind, id: String): File {
        return File(kindDir(kind), Hash.sha256(id).take(INSTALL_DIR_NAME_LENGTH))
    }

    /** Every install of [kind]; one whose manifest can't be read is listed by its folder. */
    fun listInstalled(kind: FileExtensionKind): List<StoredFileExtension> {
        return kindDir(kind).listFiles()
            .orEmpty()
            .filter { it.isDirectory && File(it, MANIFEST_FILE).isFile }
            .map { readInstall(kind, it) }
    }

    fun readInstalled(kind: FileExtensionKind, id: String): StoredFileExtension? {
        val dir = installDir(kind, id)
        return readInstall(kind, dir).takeIf { File(dir, MANIFEST_FILE).isFile }
    }

    /**
     * Removes the install of [id], or one whose manifest couldn't be read and which was listed by its folder's name
     * (see [StoredFileExtension.Unreadable]).
     */
    fun delete(kind: FileExtensionKind, id: String) {
        installDir(kind, id).deleteRecursively()
        File(kindDir(kind), id)
            .takeIf { it.isDirectory && readInstall(kind, it) is StoredFileExtension.Unreadable }
            ?.deleteRecursively()
    }

    private fun readInstall(kind: FileExtensionKind, dir: File): StoredFileExtension {
        return try {
            val manifest = json.decodeFromString<FileExtensionManifest>(File(dir, MANIFEST_FILE).readText())
            StoredFileExtension.Readable(InstalledFileExtension(kind, manifest, File(dir, manifest.sha256)))
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Can't read the file extension in ${dir.path}" }
            StoredFileExtension.Unreadable(kind, dir.name)
        }
    }

    private fun kindDir(kind: FileExtensionKind) = File(root, kind.directoryName)

    companion object {
        const val MANIFEST_FILE = "manifest.json"

        // 64 bits of the hash: unique enough for one kind's IDs, short enough to read in a path
        private const val INSTALL_DIR_NAME_LENGTH = 16

        val json = Json { ignoreUnknownKeys = true }
    }
}

internal sealed interface StoredFileExtension {
    val kind: FileExtensionKind

    data class Readable(val extension: InstalledFileExtension) : StoredFileExtension {
        override val kind get() = extension.kind
    }

    /** All that's known is its folder's name, which stands in for its ID. */
    data class Unreadable(override val kind: FileExtensionKind, val dirName: String) : StoredFileExtension
}
