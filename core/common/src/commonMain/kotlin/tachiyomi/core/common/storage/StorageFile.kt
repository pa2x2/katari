package tachiyomi.core.common.storage

import mihon.core.common.platform.PlatformFacility
import java.io.InputStream
import java.io.OutputStream

/**
 * A file or folder in the app's storage: on Android over UniFile, which reaches both plain files and the document
 * folders the user picks, and on desktop over plain files.
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

    /** The file's path where the storage has one, otherwise its [uri]: what messages show the user. */
    val displayablePath: String

    val isDirectory: Boolean

    val isFile: Boolean

    fun exists(): Boolean

    /** In bytes; 0 where the file is missing or the storage doesn't know. */
    fun length(): Long

    /** Milliseconds since the epoch; 0 where the file is missing or the storage doesn't know. */
    fun lastModified(): Long

    /** Null where this is no folder or can't be listed. */
    fun listFiles(): List<StorageFile>?

    /**
     * The folder's files with their kind, size and modification time, read in a fixed number of storage queries where
     * the storage allows it. Reading those file by file costs an Android document folder a query per file and
     * property: thousands for a folder of video segments. Empty where this is no folder or can't be listed.
     */
    fun listWithDetails(): List<ListedStorageFile>

    /** Null where the folder has no child named [name]. */
    fun findFile(name: String): StorageFile?

    /** The file named [name] in this folder, made empty if it's missing; null where a folder has the name. */
    fun createFile(name: String): StorageFile?

    /** The folder named [name] in this folder, made if it's missing; null where a file has the name. */
    fun createDirectory(name: String): StorageFile?

    /**
     * Gives the file or folder the name [name] in the same folder. Returns it under the new name, or null where the
     * rename failed. Callers carry on with the returned file: whether this one follows the rename differs by storage.
     */
    fun renameTo(name: String): StorageFile?

    /** Deletes the file, or the folder and everything in it. */
    fun delete(): Boolean

    /** Bytes free on the storage that holds the file; -1 where the storage doesn't say. */
    fun availableBytes(): Long

    fun openInputStream(): InputStream

    /** Replaces what the file holds, or writes after its end when [append]. */
    fun openOutputStream(append: Boolean = false): OutputStream
}

val StorageFile.extension: String?
    get() = name?.substringAfterLast('.')

val StorageFile.nameWithoutExtension: String?
    get() = name?.substringBeforeLast('.')
