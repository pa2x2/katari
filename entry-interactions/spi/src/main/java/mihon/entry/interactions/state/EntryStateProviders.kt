package mihon.entry.interactions.state

import mihon.entry.interactions.media.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.runtime.EntryInteractionProvider
import mihon.entry.interactions.runtime.entryInteractionCapability
import mihon.feature.graph.CapabilityId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryMigrationProvider : EntryInteractionProvider

val EntryMigrationCapability =
    entryInteractionCapability<EntryMigrationProvider>(
        id = CapabilityId("entry.migration"),
    )

interface EntryConsumptionProcessor : EntryInteractionProvider {
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

val EntryConsumptionCapability =
    entryInteractionCapability<EntryConsumptionProcessor>(
        id = CapabilityId("entry.consumption"),
    )

interface EntryBookmarkProcessor : EntryInteractionProvider {
    suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean)
}

val EntryBookmarkCapability =
    entryInteractionCapability<EntryBookmarkProcessor>(
        id = CapabilityId("entry.bookmarking"),
    )

interface EntryProgressProcessor : EntryInteractionProvider {
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

val EntryProgressCapability =
    entryInteractionCapability<EntryProgressProcessor>(
        id = CapabilityId("entry.progress-transfer"),
    )

interface EntryPlaybackPreferencesProcessor : EntryInteractionProvider {
    suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot?
    suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot)

    /** Returns whether stored source preferences were copied. */
    suspend fun copy(sourceEntry: Entry, targetEntry: Entry): Boolean
}

val EntryPlaybackPreferencesCapability =
    entryInteractionCapability<EntryPlaybackPreferencesProcessor>(
        id = CapabilityId("entry.playback-preferences-transfer"),
    )
