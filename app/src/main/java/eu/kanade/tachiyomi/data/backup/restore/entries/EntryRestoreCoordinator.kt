package eu.kanade.tachiyomi.data.backup.restore.entries

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.restore.restorers.EntryRestorer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreFinalization
import tachiyomi.data.Database

internal class EntryRestoreCoordinator(
    private val transaction: suspend (block: suspend () -> Unit) -> Unit,
    private val sortByNew: suspend (List<BackupEntry>) -> List<BackupEntry>,
    private val restoreEntry: suspend (BackupEntry, List<BackupCategory>) -> Unit,
    private val finalizeRestore: suspend (Long) -> EntryBackupRestoreFinalization,
) {

    constructor(database: Database, entryRestorer: EntryRestorer) : this(
        transaction = { block -> database.transaction { block() } },
        sortByNew = entryRestorer::sortByNew,
        restoreEntry = entryRestorer::restore,
        finalizeRestore = entryRestorer::finalizeFeatureRestore,
    )

    suspend fun restoreEntries(
        destinationProfileId: Long,
        backupEntries: List<BackupEntry>,
        backupCategories: List<BackupCategory>,
        onBatchFailure: (Exception) -> Unit,
        onEntryFailure: (BackupEntry, Exception) -> Unit,
        onProgress: (lastEntry: BackupEntry, restoredCount: Int) -> Unit,
    ): EntryBackupRestoreFinalization {
        sortByNew(backupEntries)
            .chunked(100)
            .forEach { chunk ->
                val restoredAsBatch = try {
                    transaction {
                        chunk.forEach {
                            currentCoroutineContext().ensureActive()
                            restoreEntry(it, backupCategories)
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
                            restoreEntry(it, backupCategories)
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
