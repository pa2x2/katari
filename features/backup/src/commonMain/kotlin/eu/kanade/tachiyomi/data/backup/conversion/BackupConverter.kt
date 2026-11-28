package eu.kanade.tachiyomi.data.backup.conversion

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.storage.BackupDocument
import eu.kanade.tachiyomi.data.backup.storage.BackupDocuments
import eu.kanade.tachiyomi.data.backup.storage.BackupReadCounter
import eu.kanade.tachiyomi.data.backup.storage.BackupReadProgress
import eu.kanade.tachiyomi.data.backup.storage.BackupWorkingFiles
import eu.kanade.tachiyomi.data.backup.stream.BackupContainer
import eu.kanade.tachiyomi.data.backup.stream.BackupLayout
import eu.kanade.tachiyomi.data.backup.stream.BackupWriter
import eu.kanade.tachiyomi.data.backup.stream.ProtoInput
import eu.kanade.tachiyomi.data.backup.stream.protobuf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.protobuf.ProtoBuf
import okio.buffer
import okio.gzip
import okio.sink
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import java.io.File
import java.io.IOException

/**
 * Turns other apps' backups into Katari backups, which restore as any Katari backup does. Each [BackupFormat] knows one
 * family of apps; the converter finds which could have written a file and runs the one the user settles on.
 */
@Inject
class BackupConverter(
    private val context: PlatformContext,
    private val parser: ProtoBuf,
    private val documents: BackupDocuments,
    private val workingFiles: BackupWorkingFiles,
    private val restoreRunner: BackupRestoreRunner,
    formats: Set<BackupFormat>,
) {
    /** Every format, in the order the tool lists them. */
    val formats: List<BackupFormat> = formats.sortedBy { it.appName.lowercase() }

    /** Every app the tool reads backups of, each family's own app before its forks. */
    val apps: List<BackupApp> = this.formats.flatMap { format ->
        (listOf(format.appName) + format.relatedAppNames).map { BackupApp(it, format) }
    }

    /** The name and size of the file at [uri]. */
    suspend fun describe(uri: DocumentUri): BackupDocument = withContext(Dispatchers.IO) {
        readable { documents.describe(uri) }
    }

    /**
     * Which app wrote the backup at [uri], telling [onProgress] how far reading it has got. Another app's backup is read
     * to its end, and throws when it is damaged or old JSON. A Katari backup is told from its first field, as Katari
     * writes a backup's profiles or entries before anything else; restoring it reads it to its end, which finds any
     * damage after that.
     */
    suspend fun identify(
        uri: DocumentUri,
        onProgress: (BackupReadProgress) -> Unit,
    ): BackupIdentity = withContext(Dispatchers.IO) {
        if (readable { startsAsKatari(uri) }) return@withContext BackupIdentity.Katari
        val counter = BackupReadCounter(documents.describe(uri).size, reads = 1, onProgress)
        val contents = readable { counter.counted(documents.read(uri)).buffer().use(::readBackupFileContents) }
        if (contents == BackupFileContents.Json) {
            throw IOException(context.stringResource(MR.strings.invalid_backup_file_json))
        }
        val isKatari = contents is BackupFileContents.Protobuf &&
            contents.fieldNumbers.any { it in BackupLayout.KATARI_FIELD_NUMBERS }
        if (isKatari) return@withContext BackupIdentity.Katari
        val fitting = formats.filter { it.recognizes(contents) }
        if (fitting.isEmpty()) {
            BackupIdentity.Unsupported
        } else {
            BackupIdentity.Foreign(fitting.map { BackupApp(it.appWriting(contents), it) })
        }
    }

    /**
     * Converts the backup at [uri] as [app]'s into a Katari backup in the app's cache, telling [onProgress] how far it
     * has got. The file stays until the next conversion, so a restore started from it can read it to the end. It isn't
     * compressed: it is read once more by the restore and then deleted, and [save] compresses the copy it keeps.
     */
    suspend fun convert(
        uri: DocumentUri,
        app: BackupApp,
        onProgress: (BackupReadProgress) -> Unit,
    ): ConvertedBackup = withContext(Dispatchers.IO) {
        val format = app.format
        val file = newConvertedFile()
        try {
            val content = file.sink().buffer().use { sink ->
                val writer = BackupWriter(sink, parser, workingFiles.scratchDirectory)
                val counter = BackupReadCounter(documents.describe(uri).size, format.fileReads, onProgress)
                val open = {
                    counter.counted(documents.read(uri)).buffer()
                        .let { source -> BackupContainer.of(source).protobuf(source) }
                }
                val content = try {
                    format.convert(open, app.name) { entry ->
                        writer.writeEntry(entry)
                        counter.countEntry()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    throw convertingFailure(uri, app, e)
                }
                writer.writeMetadata(content.metadata.copy(convertedFrom = app.name))
                content
            }
            ConvertedBackup(DocumentUri(file.toURI().toString()), content.report)
        } catch (e: Throwable) {
            file.delete()
            throw e
        }
    }

    /** Copies [converted] to [to], a file the user picked to keep it in, gzipped as every Katari backup is. */
    suspend fun save(converted: ConvertedBackup, to: DocumentUri) = withContext(Dispatchers.IO) {
        documents.read(converted.uri).buffer().use { source ->
            documents.write(to).gzip().buffer().use { it.writeAll(source) }
        }
    }

    // A format fails in whatever way the file gives way under it: a file it doesn't recognize wasn't written by the app
    // (D110); one it does, or one that can't be read again, is damaged
    private fun convertingFailure(uri: DocumentUri, app: BackupApp, failure: Exception): Exception {
        val recognized = runCatching {
            documents.read(uri).buffer().use(::readBackupFileContents).let(app.format::recognizes)
        }.getOrNull()
        return if (recognized == false) {
            NotAppBackupException(app, failure)
        } else {
            IOException(context.stringResource(MR.strings.invalid_backup_file_unknown), failure)
        }
    }

    // Katari writes a backup's profiles or entries before anything else
    private fun startsAsKatari(uri: DocumentUri): Boolean = documents.read(uri).buffer().use { source ->
        val container = BackupContainer.of(source)
        val protobuf = container == BackupContainer.GZIP || container == BackupContainer.RAW
        protobuf && container.protobuf(source).let { content ->
            !content.exhausted() && (ProtoInput(content).readTag() ushr 3).toInt() in BackupLayout.KATARI_FIELD_NUMBERS
        }
    }

    private suspend fun newConvertedFile(): File {
        val folder = File(workingFiles.scratchDirectory, CONVERTED_FOLDER)
        // Only one restore runs at a time, and nothing but a restore reads converted backups
        if (!restoreRunner.isRunning()) folder.listFiles()?.forEach(File::delete)
        folder.mkdirs()
        return File.createTempFile("converted", ".tachibk", folder)
    }

    // Reading a damaged file fails in whatever way its container or encoding gives way
    private inline fun <T> readable(block: () -> T): T {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown), e)
        }
    }

    private companion object {
        const val CONVERTED_FOLDER = "converted-backups"
    }
}

sealed interface BackupIdentity {
    /** A Katari backup, which restores without converting. */
    data object Katari : BackupIdentity

    /**
     * Another app's backup, which any of [apps] could have written, each the own app of a family whose format reads the
     * file: one names the app, more leave the user to say which.
     */
    data class Foreign(val apps: List<BackupApp>) : BackupIdentity

    /** No format the tool knows wrote it. */
    data object Unsupported : BackupIdentity
}

/** The file converted as [app]'s isn't a backup that app wrote. */
class NotAppBackupException(val app: BackupApp, cause: Throwable) : IOException(cause)

/** A Katari backup converted from another app's, at [uri] in the app's cache, and what came over into it. */
class ConvertedBackup(val uri: DocumentUri, val report: ConversionReport)
