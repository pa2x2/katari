package mihon.entry.interactions.state

import mihon.entry.interactions.media.EntryPlaybackPreferencesSnapshot
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryMigrationProvider

interface EntryConsumptionProcessor {
    /** Returns exactly the children whose persisted consumed state changed. */
    suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean): List<EntryChapter>
}

/**
 * Shared state-transition semantics for Consumption providers and the feature coordinator.
 * Provider presence remains the only support authority.
 */
fun shouldChangeConsumption(status: EntryConsumptionStatus, consumed: Boolean): Boolean {
    return when (consumed) {
        true -> !status.consumed
        false -> status.consumed || status.hasPartialProgress
    }
}

interface EntryBookmarkProcessor {
    suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean)
}

interface EntryProgressProcessor {
    suspend fun snapshot(entry: Entry): EntryProgressSnapshot
    suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot)
    suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    )

    /**
     * Captures target-ready progress for durable Migration delivery.
     *
     * Entry types whose child identity differs from their persisted media identity may override this
     * without replacing the shared Entry Migration transaction.
     */
    suspend fun prepareMigration(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ): EntryProgressSnapshot {
        val sourceStates = snapshot(sourceEntry).states.associateBy { it.contentKey to it.resourceKey }
        return EntryProgressSnapshot(
            resourceMappings.mapNotNull { mapping ->
                sourceStates[mapping.sourceContentKey to mapping.sourceResourceKey]?.copy(
                    contentKey = mapping.targetContentKey,
                    resourceKey = mapping.targetResourceKey,
                    sourceChildKey = mapping.targetResourceKey,
                )
            },
        )
    }
}

interface EntryPlaybackPreferencesProcessor {
    suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot?
    suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot)

    /** Returns whether stored source preferences were copied. */
    suspend fun copy(sourceEntry: Entry, targetEntry: Entry): Boolean
}

/** Declares that an Entry type's readers translate text, so each series can keep its own translation languages. */
interface EntryTranslationLanguagesProvider
