package mihon.entry.interactions.library

import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

/** Genuine media-specific progress evidence used by the shared Library Progress feature. */
interface EntryLibraryProgressProvider {
    suspend fun evidence(entry: Entry, chapters: List<EntryChapter>): EntryLibraryProgressEvidence

    suspend fun evidence(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryLibraryProgressEvidence = evidence(entry, chapters)
}

data class EntryLibraryProgressEvidence(
    val hasMediaProgress: Boolean,
    val inProgressItemId: Long?,
    val inProgressFraction: Float?,
    val lastActivityAt: Long,
)
