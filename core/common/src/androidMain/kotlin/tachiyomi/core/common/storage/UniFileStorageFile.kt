package tachiyomi.core.common.storage

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.DiskUtil
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * A storage file over UniFile. [context] lets [listWithDetails] read a document folder in one provider query; files
 * made without one list file by file. The files this one finds, makes and lists carry it on.
 */
class UniFileStorageFile(val file: UniFile, private val context: Context? = null) : StorageFile {
    override val name: String?
        get() = file.name

    override val uri: String
        get() = file.uri.toString()

    override val displayablePath: String
        get() = file.filePath ?: uri

    override val isDirectory: Boolean
        get() = file.isDirectory

    override val isFile: Boolean
        get() = file.isFile

    override fun exists(): Boolean = file.exists()

    override fun length(): Long = file.length()

    override fun lastModified(): Long = file.lastModified()

    override fun listFiles(): List<StorageFile>? = file.listFiles()?.map(::child)

    override fun listWithDetails(): List<ListedStorageFile> =
        DirectoryListing(context).list(file).map { listed ->
            ListedStorageFile(
                file = child(listed.file),
                name = listed.name,
                isDirectory = listed.isDirectory,
                length = listed.length,
                lastModified = listed.lastModified,
            )
        }

    override fun findFile(name: String): StorageFile? = file.findFile(name)?.let(::child)

    override fun createFile(name: String): StorageFile? = file.createFile(name)?.let(::child)

    override fun createDirectory(name: String): StorageFile? = file.createDirectory(name)?.let(::child)

    // UniFile renames the file in place, so this wrapper names the renamed file too.
    override fun renameTo(name: String): StorageFile? = takeIf { file.renameTo(name) }

    override fun delete(): Boolean = file.delete()

    override fun availableBytes(): Long = DiskUtil.getAvailableStorageSpace(file)

    override fun openInputStream(): InputStream = file.openInputStream()

    // Some document providers open a document for writing without emptying it, which would leave the end of a longer
    // old content behind what is written.
    override fun openOutputStream(append: Boolean): OutputStream = file.openOutputStream(append).also { output ->
        if (!append) (output as? FileOutputStream)?.channel?.truncate(0)
    }

    private fun child(file: UniFile) = UniFileStorageFile(file, context)
}

/**
 * The UniFile behind a storage file, for the code that still reads UniFile (the readers and the downloads, until they
 * move onto [StorageFile]). Every storage file on Android is a [UniFileStorageFile].
 */
fun StorageFile.toUniFile(): UniFile = (this as UniFileStorageFile).file
