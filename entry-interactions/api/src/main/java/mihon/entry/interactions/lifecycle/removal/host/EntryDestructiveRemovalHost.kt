package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

interface EntryDestructiveRemovalHost {
    suspend fun remove(
        requested: List<Entry>,
        beforeDelete: suspend (persisted: List<Entry>) -> Unit,
    ): EntryDestructiveRemovalCommit
}

sealed interface EntryDestructiveRemovalCommit {
    data class Applied(val entries: List<Entry>) : EntryDestructiveRemovalCommit
    data object NoChange : EntryDestructiveRemovalCommit
    data object Conflict : EntryDestructiveRemovalCommit
}

fun interface EntryDestructiveRemovalCustomCoverHost {
    suspend fun removeCustomCover(entry: Entry)
}
