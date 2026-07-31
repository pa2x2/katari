package mihon.entry.interactions.library

import mihon.entry.interactions.runtime.EntryInteractionProvider
import mihon.entry.interactions.runtime.entryInteractionCapability
import mihon.feature.graph.CapabilityId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Genuine media-specific progress evidence used by the shared Library Progress feature. */
interface EntryLibraryProgressProvider : EntryInteractionProvider {
    suspend fun evidence(entry: Entry, chapters: List<EntryChapter>): EntryLibraryProgressEvidence
}

data class EntryLibraryProgressEvidence(
    val hasMediaProgress: Boolean,
    val inProgressItemId: Long?,
    val inProgressFraction: Float?,
    val lastActivityAt: Long,
)

val EntryLibraryProgressCapability =
    entryInteractionCapability<EntryLibraryProgressProvider>(
        id = CapabilityId("entry.library-progress"),
    )
