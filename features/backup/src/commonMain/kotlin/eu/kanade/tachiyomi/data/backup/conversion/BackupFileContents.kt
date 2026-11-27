package eu.kanade.tachiyomi.data.backup.conversion

import eu.kanade.tachiyomi.data.backup.stream.BackupContainer
import eu.kanade.tachiyomi.data.backup.stream.MalformedBackupException
import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.WIRE_TYPE_LENGTH_DELIMITED
import eu.kanade.tachiyomi.data.backup.stream.protobuf
import okio.BufferedSource
import java.util.zip.ZipInputStream

/** What a backup file holds, as far as telling which app wrote it needs. */
sealed interface BackupFileContents {
    /**
     * Protobuf, gzipped or not, with the numbers of the fields at its top level and, by top-level field, those inside
     * the messages [INNER_FIELD_NUMBERS] lists, whose own fields tell one app of a family from another (D109).
     */
    data class Protobuf(val fieldNumbers: Set<Int>, val innerFieldNumbers: Map<Int, Set<Int>>) : BackupFileContents

    /** A zip archive with the names of its entries. */
    data class Zip(val entryNames: Set<String>) : BackupFileContents

    /** JSON, as Tachiyomi wrote backups before protobuf. */
    data object Json : BackupFileContents
}

/**
 * Reads [source] to its end. A file cut short or corrupt anywhere throws, even where everything before the damage reads
 * as whole fields: converting only part of a backup would restore part of a library without saying so.
 */
internal fun readBackupFileContents(source: BufferedSource): BackupFileContents {
    return when (val container = BackupContainer.of(source)) {
        BackupContainer.JSON -> BackupFileContents.Json
        BackupContainer.ZIP -> BackupFileContents.Zip(zipEntryNames(source))
        BackupContainer.GZIP, BackupContainer.RAW -> protobufContents(container.protobuf(source))
    }
}

private fun protobufContents(source: BufferedSource): BackupFileContents.Protobuf {
    val input = ProtoInput(source)
    val fieldNumbers = mutableSetOf<Int>()
    val innerFieldNumbers = mutableMapOf<Int, MutableSet<Int>>()
    while (!input.exhausted()) {
        val tag = input.readTag()
        val number = (tag ushr 3).toInt()
        fieldNumbers += number
        if (number in INNER_FIELD_NUMBERS && (tag and 0x7).toInt() == WIRE_TYPE_LENGTH_DELIMITED) {
            val end = input.readLength() + input.position
            val inner = innerFieldNumbers.getOrPut(number, ::mutableSetOf)
            while (input.position < end) {
                val innerTag = input.readTag()
                inner += (innerTag ushr 3).toInt()
                input.skipField(innerTag)
            }
            if (input.position != end) throw MalformedBackupException("A field runs past its message")
        } else {
            input.skipField(tag)
        }
    }
    return BackupFileContents.Protobuf(fieldNumbers, innerFieldNumbers)
}

/**
 * The top-level fields of the Mihon and Aniyomi families whose messages hold the parts only one fork writes: entries (1,
 * Aniyomi's legacy anime at 3, its anime at 501), categories (2, legacy anime 4, anime 502) and Aniyomi's manga
 * extension stores (106). research/10 cites each fork's numbers.
 */
internal val INNER_FIELD_NUMBERS = setOf(1, 2, 3, 4, 106, 501, 502)

// Reading every entry through makes a cut or corrupt entry throw
private fun zipEntryNames(source: BufferedSource): Set<String> {
    val zip = ZipInputStream(source.inputStream())
    return buildSet {
        while (true) {
            val entry = zip.nextEntry ?: break
            add(entry.name)
            zip.skip(Long.MAX_VALUE)
        }
    }
}
