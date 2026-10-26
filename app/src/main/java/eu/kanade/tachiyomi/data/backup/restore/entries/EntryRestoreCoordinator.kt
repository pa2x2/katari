package eu.kanade.tachiyomi.data.backup.restore.entries

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.restore.restorers.EntryRestorer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.chunked
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreFinalization
import mihon.entry.interactions.persistence.backup.EntryBackupRestoreSession
import tachiyomi.data.Database

internal class EntryRestoreCoordinator(
    private val transaction: suspend (block: suspend () -> Unit) -> Unit,
    private val restoreEntry: suspend (profileId: Long, BackupEntry, List<BackupCategory>) -> Unit,
    private val finalizeRestore: suspend (Long) -> EntryBackupRestoreFinalization,
) {

    constructor(database: Database, entryRestorer: EntryRestorer, session: EntryBackupRestoreSession) : this(
        transaction = { block -> database.transaction { block() } },
        restoreEntry = { profileId, entry, categories -> entryRestorer.restore(session, profileId, entry, categories) },
        finalizeRestore = { profileId -> entryRestorer.finalizeFeatureRestore(session, profileId) },
    )

    suspend fun restoreEntries(
        destinationProfileId: Long,
        backupEntries: Flow<BackupEntry>,
        backupCategories: List<BackupCategory>,
        onBatchFailure: (Exception) -> Unit,
        onEntryFailure: (BackupEntry, Exception) -> Unit,
        onProgress: (lastEntry: BackupEntry, restoredCount: Int) -> Unit,
    ): EntryBackupRestoreFinalization {
        backupEntries
            .chunked(100)
            .collect { chunk ->
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
