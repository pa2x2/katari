package app.katari.entry.interactions.library

import app.katari.entry.interactions.runtime.requireProcessor
import eu.kanade.tachiyomi.source.entry.EntryType
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
        return provider.evidence(entry, chapters)
    }

    override suspend fun evidence(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryLibraryProgressEvidence {
        val provider = providers.requireProcessor("library progress", entry.type)
        return provider.evidence(entry, chapters, progressStates)
    }
}
