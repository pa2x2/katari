package eu.kanade.tachiyomi.data.backup

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.storage.BackupDocuments
import eu.kanade.tachiyomi.data.backup.stream.BackupContainer
import eu.kanade.tachiyomi.data.backup.stream.BackupReader
import eu.kanade.tachiyomi.data.backup.stream.BackupSummary
import eu.kanade.tachiyomi.data.backup.stream.BackupSummaryBuilder
import eu.kanade.tachiyomi.data.backup.stream.MalformedBackupException
import eu.kanade.tachiyomi.data.backup.stream.protobuf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.protobuf.ProtoBuf
import okio.buffer
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import java.io.IOException

/** Reads potentially-gzipped backups, never holding more than one of their entries at a time. */
@Inject
class BackupDecoder(
    private val context: PlatformContext,
    private val parser: ProtoBuf,
    private val documents: BackupDocuments,
) {
    internal suspend fun summarize(uri: DocumentUri): BackupSummary = withContext(Dispatchers.IO) {
        val summary = BackupSummaryBuilder()
        val metadata = read(uri) { reader -> reader.read(include = { true }, onEntry = summary::add) }
        summary.build(metadata)
    }

    /**
     * The entries of the profile at [profileIndex] of [Backup.backupProfiles], or of the backup's top level when it is
     * null, one at a time.
     */
    internal fun decodeEntries(uri: DocumentUri, profileIndex: Int?): Flow<BackupEntry> = flow {
        read(uri) { reader ->
            reader.read(include = { it == profileIndex }) { entry -> emit(entry.decode()) }
        }
    }
        .flowOn(Dispatchers.IO)

    private suspend fun <T> read(uri: DocumentUri, block: suspend (BackupReader) -> T): T {
        return documents.read(uri).buffer().use { source ->
            val container = BackupContainer.of(source)
            if (container == BackupContainer.JSON) {
                throw IOException(context.stringResource(MR.strings.invalid_backup_file_json))
            }
            val backupSource = container.protobuf(source)

            try {
                backupSource.use { block(BackupReader(it, parser)) }
            } catch (e: MalformedBackupException) {
                throw IOException(context.stringResource(MR.strings.invalid_backup_file_unknown), e)
            }
        }
    }
}
