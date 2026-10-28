package eu.kanade.tachiyomi.data.backup.stream

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.protobuf.ProtoBuf
import okio.Buffer

/** A backup's bytes as restoring reads them: its summary, and every entry wherever it sits, in file order. */
internal class ReadBackup(val summary: BackupSummary, val entries: List<EncodedBackupEntry>) {
    val decodedEntries: List<BackupEntry> by lazy { entries.map { it.decode() } }
}

internal fun readBackup(bytes: ByteArray): ReadBackup = runBlocking {
    val summary = BackupSummaryBuilder()
    val entries = mutableListOf<EncodedBackupEntry>()
    val metadata = BackupReader(Buffer().write(bytes), ProtoBuf).read(include = { true }) { entry ->
        summary.add(entry)
        entries += entry
    }
    ReadBackup(summary.build(metadata), entries)
}

internal fun encode(backup: Backup): ByteArray = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)
