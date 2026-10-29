package tachiyomi.core.common.storage

import com.hippo.unifile.UniFile
import java.io.InputStream
import java.io.OutputStream

class UniFileStorageFile(val file: UniFile) : StorageFile {
    override val name: String?
        get() = file.name

    override val uri: String
        get() = file.uri.toString()

    override val isDirectory: Boolean
        get() = file.isDirectory

    override val isFile: Boolean
        get() = file.isFile

    override fun lastModified(): Long = file.lastModified()

    override fun listFiles(): List<StorageFile>? = file.listFiles()?.map(::UniFileStorageFile)

    override fun findFile(name: String): StorageFile? = file.findFile(name)?.let(::UniFileStorageFile)

    override fun createFile(name: String): StorageFile? = file.createFile(name)?.let(::UniFileStorageFile)

    override fun delete(): Boolean = file.delete()

    override fun openInputStream(): InputStream = file.openInputStream()

    override fun openOutputStream(): OutputStream = file.openOutputStream()
}

/**
 * The UniFile behind a storage file, for the code that still reads UniFile (the readers, until they move onto
 * [StorageFile]). Every storage file on Android is a [UniFileStorageFile].
 */
fun StorageFile.toUniFile(): UniFile = (this as UniFileStorageFile).file
