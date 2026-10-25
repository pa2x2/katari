package eu.kanade.tachiyomi.data.backup.restore.entries

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.restore.restorers.EntryRestorer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreFinalization
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSession
import tachiyomi.data.Database

internal class EntryRestoreCoordinator(
    private val transaction: suspend (block: suspend () -> Unit) -> Unit,
    private val sortByNew: suspend (profileId: Long, List<BackupEntry>) -> List<BackupEntry>,
    private val restoreEntry: suspend (profileId: Long, BackupEntry, List<BackupCategory>) -> Unit,
    private val finalizeRestore: suspend (Long) -> EntryBackupRestoreFinalization,
) {

    constructor(database: Database, entryRestorer: EntryRestorer, session: EntryBackupRestoreSession) : this(
        transaction = { block -> database.transaction { block() } },
        sortByNew = entryRestorer::sortByNew,
        restoreEntry = { profileId, entry, categories -> entryRestorer.restore(session, profileId, entry, categories) },
        finalizeRestore = { profileId -> entryRestorer.finalizeFeatureRestore(session, profileId) },
    )

    suspend fun restoreEntries(
        destinationProfileId: Long,
        backupEntries: List<BackupEntry>,
        backupCategories: List<BackupCategory>,
        onBatchFailure: (Exception) -> Unit,
        onEntryFailure: (BackupEntry, Exception) -> Unit,
        onProgress: (lastEntry: BackupEntry, restoredCount: Int) -> Unit,
    ): EntryBackupRestoreFinalization {
        sortByNew(destinationProfileId, backupEntries)
            .chunked(100)
            .forEach { chunk ->
                val restoredAsBatch = try {
                    transaction {
                        chunk.forEach {
                            currentCoroutineContext().ensureActive()
                            restoreEntry(destinationProfileId, it, backupCategories)
                        }
                    }
                    true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onBatchFailure(e)
                    false
                }

                if (!restoredAsBatch) {
                    chunk.forEach {
                        currentCoroutineContext().ensureActive()
                        try {
                            restoreEntry(destinationProfileId, it, backupCategories)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            onEntryFailure(it, e)
                        }
                    }
                }

                onProgress(chunk.last(), chunk.size)
            }

        return finalizeRestore(destinationProfileId)
    }
}
