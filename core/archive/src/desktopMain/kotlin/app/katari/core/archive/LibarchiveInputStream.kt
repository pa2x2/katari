package app.katari.core.archive

import com.sun.jna.Memory
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import kotlin.concurrent.Volatile

/** One pass through the archive at [path]: [getNextEntry] moves to an entry, and reading gives that entry's data. */
internal class LibarchiveInputStream(private val library: Libarchive, path: Path) : InputStream() {
    private val lock = Any()

    @Volatile
    private var isClosed = false

    private val archive: Pointer = library.readNew() ?: throw IOException("libarchive couldn't allocate an archive")

    private val buffer = Memory(BUFFER_SIZE)

    private val entry = PointerByReference()

    init {
        try {
            requireSuccess(library.readSupportFilterAll(archive))
            requireSuccess(library.readSupportFormatAll(archive))
            requireSuccess(library.readOpenFilename(archive, path.toString(), NativeLong(BLOCK_SIZE)))
        } catch (e: IOException) {
            close()
            throw e
        }
    }

    override fun read(): Int {
        val byte = ByteArray(1)
        return if (read(byte, 0, 1) == -1) -1 else byte[0].toUByte().toInt()
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        val size = library.readData(archive, buffer, NativeLong(minOf(len.toLong(), BUFFER_SIZE))).toInt()
        if (size < 0) throw IOException(errorMessage(size))
        if (size == 0) return -1
        buffer.read(0, b, off, size)
        return size
    }

    override fun close() {
        synchronized(lock) {
            if (isClosed) return
            isClosed = true
        }

        library.readFree(archive)
        buffer.close()
    }

    fun getNextEntry(): ArchiveEntry? {
        val result = library.readNextHeader(archive, entry)
        if (result == ARCHIVE_EOF) return null
        requireSuccess(result)
        val header = entry.value
        val name = (library.entryPathnameUtf8(header) ?: library.entryPathname(header))?.getString(0, "UTF-8")
            ?: return null
        val isFile = library.entryFiletype(header) == AE_IFREG
        return ArchiveEntry(name, isFile)
    }

    /** The kind of the entry [getNextEntry] moved to last, as the `AE_IF*` bits of its mode. */
    val entryFiletype: Int
        get() = library.entryFiletype(entry.value)

    /** Whether the entry [getNextEntry] moved to last is one its owner may run. */
    val entryIsExecutable: Boolean
        get() = library.entryPerm(entry.value) and OWNER_EXECUTE != 0

    private fun requireSuccess(result: Int) {
        if (result < ARCHIVE_WARN) throw IOException(errorMessage(result))
    }

    private fun errorMessage(result: Number): String = library.errorString(archive) ?: "libarchive error $result"
}

private const val BUFFER_SIZE = 64 * 1024L

private const val BLOCK_SIZE = 64 * 1024L

private const val OWNER_EXECUTE = 0b001_000_000
