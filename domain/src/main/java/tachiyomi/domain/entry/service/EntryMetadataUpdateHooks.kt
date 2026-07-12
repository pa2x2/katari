package tachiyomi.domain.entry.service

import tachiyomi.domain.entry.model.Entry

/** Platform hooks required when source metadata changes external entry state. */
fun interface EntryMetadataUpdateHooks {
    suspend fun onTitleChanged(entry: Entry, newTitle: String)
}
