package eu.kanade.tachiyomi.data.backup.conversion

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.storage.BackupDocument
import eu.kanade.tachiyomi.data.backup.storage.BackupDocuments
import eu.kanade.tachiyomi.data.backup.storage.BackupWorkingFiles
import eu.kanade.tachiyomi.data.backup.stream.BackupContainer
import eu.kanade.tachiyomi.data.backup.stream.BackupLayout
import eu.kanade.tachiyomi.data.backup.stream.BackupWriter
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

    /** Which app wrote the backup at [uri], read to its end. Throws for a file that is damaged or old JSON. */
    suspend fun identify(uri: DocumentUri): BackupIdentity = withContext(Dispatchers.IO) {
        val contents = readable { documents.read(uri).buffer().use(::readBackupFileContents) }
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
            BackupIdentity.Foreign(fitting.map { BackupApp(it.appName, it) })
        }
    }

    /**
     * Converts the backup at [uri] as [app]'s into a Katari backup in the app's cache. The file stays until the next
     * conversion, so a restore started from it can read it to the end.
     */
    suspend fun convert(uri: DocumentUri, app: BackupApp): ConvertedBackup = withContext(Dispatchers.IO) {
        val format = app.format
        val file = newConvertedFile()
        try {
            val content = file.sink().gzip().buffer().use { sink ->
                val writer = BackupWriter(sink, parser, workingFiles.scratchDirectory)
                val content = readable {
                    val open = {
                        documents.read(uri).buffer().let { source -> BackupContainer.of(source).protobuf(source) }
                    }
                    format.convert(open, writer::writeEntry)
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

    /** Copies [converted] to [to], a file the user picked to keep it in. */
    suspend fun save(converted: ConvertedBackup, to: DocumentUri) = withContext(Dispatchers.IO) {
        documents.read(converted.uri).buffer().use { source ->
            documents.write(to).buffer().use { it.writeAll(source) }
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

/** A Katari backup converted from another app's, at [uri] in the app's cache, and what came over into it. */
class ConvertedBackup(val uri: DocumentUri, val report: ConversionReport)
