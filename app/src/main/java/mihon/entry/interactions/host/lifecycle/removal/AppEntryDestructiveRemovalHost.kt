package mihon.entry.interactions.host.lifecycle.removal

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import mihon.entry.interactions.EntryDestructiveRemovalCommit
import mihon.entry.interactions.EntryDestructiveRemovalHost
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.entry.EntryMapper
import tachiyomi.domain.entry.model.Entry

class AppEntryDestructiveRemovalHost(
    private val handler: DatabaseHandler,
) : EntryDestructiveRemovalHost {
    override suspend fun remove(
        requested: List<Entry>,
        beforeDelete: suspend (persisted: List<Entry>) -> Unit,
    ): EntryDestructiveRemovalCommit {
        return handler.await(inTransaction = true) {
            val persisted = requested.map { entry ->
                entriesQueries.getEntryById(entry.id, entry.profileId, EntryMapper::mapEntry)
                    .awaitAsOneOrNull()
                    ?: return@await EntryDestructiveRemovalCommit.Conflict
            }
            if (persisted.isEmpty()) return@await EntryDestructiveRemovalCommit.NoChange
            if (persisted.zip(requested).any { (actual, expected) ->
                    actual.type != expected.type || actual.source != expected.source || actual.url != expected.url
                }
            ) {
                return@await EntryDestructiveRemovalCommit.Conflict
            }

            beforeDelete(persisted)
            persisted.forEach { entry -> entriesQueries.deleteById(entry.profileId, entry.id) }
            historyQueries.removeResettedHistory()
            EntryDestructiveRemovalCommit.Applied(persisted)
        }
    }
}
