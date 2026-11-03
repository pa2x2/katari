package tachiyomi.core.common.storage

import mihon.core.common.platform.PlatformFacility
import java.io.InputStream
import java.io.OutputStream

/**
 * A file or folder in the app's storage: the start of the storage interface the app's UniFile users move to, on
 * Android over UniFile and on desktop over plain files.
 *
 * It behaves as UniFile does on Android, so code moving onto it keeps its behaviour: properties ask the storage on
 * every read, listings come in the storage's own order, which is no particular one, and names compare as the storage
 * compares them (plain files case-sensitively; Android's document folders mostly not).
 */
@PlatformFacility
interface StorageFile {
    /** Null where the storage doesn't know it. */
    val name: String?

    /** A URI image loaders and the app's other readers open the file by, as `file://` for plain files. */
    val uri: String

    val isDirectory: Boolean

    val isFile: Boolean

    /** Milliseconds since the epoch; 0 where the file is missing or the storage doesn't know. */
    fun lastModified(): Long

    /** Null where this is no folder or can't be listed. */
    fun listFiles(): List<StorageFile>?

    /** Null where the folder has no child named [name]. */
    fun findFile(name: String): StorageFile?

    /** The file named [name] in this folder, made empty if it's missing; null where a folder has the name. */
    fun createFile(name: String): StorageFile?

    /** Deletes the file, or the folder and everything in it. */
    fun delete(): Boolean

    fun openInputStream(): InputStream

    /** Writes the file over from its start. */
    fun openOutputStream(): OutputStream
}

val StorageFile.extension: String?
    get() = name?.substringAfterLast('.')

val StorageFile.nameWithoutExtension: String?
    get() = name?.substringBeforeLast('.')
