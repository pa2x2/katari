package mihon.entry.interactions.library

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.requireMatchingEntryType
import mihon.entry.interactions.runtime.requireProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

interface EntryLibraryProgressInteraction {
    suspend fun evidence(entry: Entry, chapters: List<EntryChapter>): EntryLibraryProgressEvidence

    suspend fun evidence(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryLibraryProgressEvidence = evidence(entry, chapters)
}

internal class ProviderBackedEntryLibraryProgressInteraction(
    private val providers: Map<EntryType, EntryLibraryProgressProvider>,
) : EntryLibraryProgressInteraction {
    override suspend fun evidence(entry: Entry, chapters: List<EntryChapter>): EntryLibraryProgressEvidence {
        val provider = providers.requireProcessor("library progress", entry.type)
        provider.requireMatchingEntryType("library progress", entry, providers.keys)
        return provider.evidence(entry, chapters)
    }

    override suspend fun evidence(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryLibraryProgressEvidence {
        val provider = providers.requireProcessor("library progress", entry.type)
        provider.requireMatchingEntryType("library progress", entry, providers.keys)
        return provider.evidence(entry, chapters, progressStates)
    }
}
