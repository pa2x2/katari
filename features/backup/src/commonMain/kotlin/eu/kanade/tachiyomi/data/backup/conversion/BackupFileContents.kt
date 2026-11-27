package eu.kanade.tachiyomi.data.backup.conversion

import eu.kanade.tachiyomi.data.backup.stream.BackupContainer
import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.protobuf
import okio.BufferedSource
import java.util.zip.ZipInputStream

/** What a backup file holds, as far as telling which app wrote it needs. */
sealed interface BackupFileContents {
    /** Protobuf, gzipped or not, with the numbers of the fields at its top level. */
    data class Protobuf(val fieldNumbers: Set<Int>) : BackupFileContents

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
        BackupContainer.GZIP, BackupContainer.RAW -> {
            BackupFileContents.Protobuf(topLevelFieldNumbers(container.protobuf(source)))
        }
    }
}

private fun topLevelFieldNumbers(source: BufferedSource): Set<Int> {
    val input = ProtoInput(source)
    return buildSet {
        while (!input.exhausted()) {
            val tag = input.readTag()
            add((tag ushr 3).toInt())
            input.skipField(tag)
        }
    }
}

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
