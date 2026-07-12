package eu.kanade.tachiyomi.entry

import mihon.entry.interactions.EntryDownloadInteraction
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryMetadataUpdateHooks

class AppEntryMetadataUpdateHooks(
    private val entryDownloadInteraction: EntryDownloadInteraction,
) : EntryMetadataUpdateHooks {
    override suspend fun onTitleChanged(entry: Entry, newTitle: String) {
        entryDownloadInteraction.renameEntry(entry, newTitle)
    }
}
