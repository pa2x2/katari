package mihon.core.archive

import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.io.IOException

/**
 * The part of libarchive (`archive.h`, `archive_entry.h`) the readers call. Each function is the C one
 * with the `archive_` prefix and in snake case: [readNextHeader] is `archive_read_next_header`.
 */
internal interface Libarchive : Library {
    fun readNew(): Pointer?

    fun readSupportFilterAll(archive: Pointer): Int

    fun readSupportFormatAll(archive: Pointer): Int

    fun readOpenFilename(archive: Pointer, fileName: String, blockSize: NativeLong): Int

    fun readNextHeader(archive: Pointer, entry: PointerByReference): Int

    /** The number of bytes read into [buffer], 0 at the end of the entry, negative on an error. */
    fun readData(archive: Pointer, buffer: Memory, size: NativeLong): NativeLong

    fun readFree(archive: Pointer): Int

    fun errorString(archive: Pointer): String?

    fun entryPathnameUtf8(entry: Pointer): Pointer?

    fun entryPathname(entry: Pointer): Pointer?

    /** The `AE_IF*` bits of the entry's mode. */
    fun entryFiletype(entry: Pointer): Int
}

internal const val ARCHIVE_EOF = 1

/** Results below it are failures; libarchive's own examples carry on after it and the results above it. */
internal const val ARCHIVE_WARN = -20

internal const val AE_IFREG = 0x8000

/**
 * libarchive, loaded on first use: the copy the app carries for its platform, or else the system's. The app carries one
 * for x86_64 Linux only, with its compression libraries built in, so there a system's archive libraries never matter.
 * Throws [LibarchiveUnavailableException] when neither loads; that is decided once per run, so installing libarchive
 * takes effect after a restart.
 */
internal fun libarchive(): Libarchive = loadedLibarchive.getOrElse { throw LibarchiveUnavailableException(it) }

private val loadedLibarchive: Result<Libarchive> by lazy {
    val failures = mutableListOf<Throwable>()
    val names = try {
        listOf(Native.extractFromResourcePath(LIBRARY_NAME, Libarchive::class.java.classLoader).absolutePath)
    } catch (e: IOException) {
        failures += e
        emptyList()
    } + SYSTEM_LIBRARY_NAMES
    for (name in names) {
        try {
            return@lazy Result.success(Native.load(name, Libarchive::class.java, LIBRARY_OPTIONS))
        } catch (e: UnsatisfiedLinkError) {
            failures += e
        }
    }
    Result.failure(failures.reduce { first, next -> first.apply { addSuppressed(next) } })
}

private const val LIBRARY_NAME = "archive"

// The versioned name first: distributions ship the unversioned one only with libarchive's development files
private val SYSTEM_LIBRARY_NAMES = listOf("libarchive.so.13", LIBRARY_NAME)

private val LIBRARY_OPTIONS = mapOf(
    Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, method -> "archive_" + method.name.toSnakeCase() },
)

private fun String.toSnakeCase(): String = buildString {
    for (char in this@toSnakeCase) {
        if (char.isUpperCase()) append('_').append(char.lowercaseChar()) else append(char)
    }
}
