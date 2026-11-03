package mihon.entry.interactions.lifecycle.profile.host

import tachiyomi.domain.entry.model.Entry

fun interface EntryProfileMoveCustomCoverHost {
    suspend fun removeCustomCovers(entries: List<Entry>)
}
