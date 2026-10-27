package eu.kanade.tachiyomi.data.backup

import android.content.Context
import android.net.Uri
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.stream.BackupReader
import eu.kanade.tachiyomi.data.backup.stream.BackupSummary
import eu.kanade.tachiyomi.data.backup.stream.BackupSummaryBuilder
import eu.kanade.tachiyomi.data.backup.stream.MalformedBackupException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.protobuf.ProtoBuf
import okio.BufferedSource
import okio.buffer
import okio.gzip
import okio.source
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import java.io.IOException

/** Reads potentially-gzipped backups, never holding more than one of their entries at a time. */
@Inject
class BackupDecoder(
    private val context: Context,
    private val parser: ProtoBuf,
) {
    internal suspend fun summarize(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val summary = BackupSummaryBuilder()
        val metadata = read(uri) { reader -> reader.read(include = { true }, onEntry = summary::add) }
        summary.build(metadata)
    }

    /**
     * The entries of the profile at [profileIndex] of [Backup.backupProfiles], or of the backup's top level when it is
     * null, one at a time.
     */
    internal fun decodeEntries(uri: Uri, profileIndex: Int?): Flow<BackupEntry> = flow {
        read(uri) { reader ->
            reader.read(include = { it == profileIndex }) { entry -> emit(entry.decode()) }
        }
    }
        .flowOn(Dispatchers.IO)

    private suspend fun <T> read(uri: Uri, block: suspend (BackupReader) -> T): T {
        return context.contentResolver.openInputStream(uri)!!.use { inputStream ->
            val source = inputStream.source().buffer()

            val peeked = source.peek().apply {
                require(2)
            }
            val id1id2 = peeked.readShort()
            val backupSource: BufferedSource = when (id1id2.toInt()) {
                0x1f8b -> source.gzip().buffer() // 0x1f8b is gzip magic bytes
                MAGIC_JSON_SIGNATURE1, MAGIC_JSON_SIGNATURE2, MAGIC_JSON_SIGNATURE3 -> {
                    throw IOException(context.stringResource(MR.strings.invalid_backup_file_json))
                }
                else -> source
            }

            try {
                backupSource.use { block(BackupReader(it, parser)) }
            } catch (e: MalformedBackupException) {
                throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown), e)
            }
        }
    }

    companion object {
        private const val MAGIC_JSON_SIGNATURE1 = 0x7b7d // `{}`
        private const val MAGIC_JSON_SIGNATURE2 = 0x7b22 // `{"`
        private const val MAGIC_JSON_SIGNATURE3 = 0x7b0a // `{\n`
    }
}
