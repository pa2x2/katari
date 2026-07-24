package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryLibraryProgressInteraction {
    suspend fun evidence(entry: Entry, chapters: List<EntryChapter>): EntryLibraryProgressEvidence
}

internal class ProviderBackedEntryLibraryProgressInteraction(
    private val providers: Map<EntryType, EntryLibraryProgressProvider>,
) : EntryLibraryProgressInteraction {
    override suspend fun evidence(entry: Entry, chapters: List<EntryChapter>): EntryLibraryProgressEvidence {
        val provider = providers.requireProcessor("library progress", entry.type)
        provider.requireMatchingEntryType("library progress", entry, providers.keys)
        return provider.evidence(entry, chapters)
    }
}
