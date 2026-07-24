package mihon.entry.interactions

import tachiyomi.domain.entry.model.Entry

fun interface EntryProfileMoveCustomCoverHost {
    suspend fun removeCustomCovers(entries: List<Entry>)
}
