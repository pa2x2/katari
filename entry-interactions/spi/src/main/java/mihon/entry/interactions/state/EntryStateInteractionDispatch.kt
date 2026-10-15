package mihon.entry.interactions.state

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.media.EntryPlaybackPreferencesSnapshot
import mihon.entry.interactions.runtime.requireProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class ProviderBackedEntryConsumptionInteraction(
    private val consumptionProcessors: Map<EntryType, EntryConsumptionProcessor>,
) : EntryConsumptionInteraction {
    override suspend fun setConsumed(
        entry: Entry,
        chapters: List<EntryChapter>,
        consumed: Boolean,
    ): List<EntryChapter> {
        val processor = consumptionProcessors.requireProcessor("consumption", entry.type)
        return processor.setConsumed(entry, chapters, consumed)
    }
}

internal class ProviderBackedEntryBookmarkInteraction(
    private val bookmarkProcessors: Map<EntryType, EntryBookmarkProcessor>,
) : EntryBookmarkInteraction {
    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        val processor = bookmarkProcessors.requireProcessor("bookmark", entry.type)
        processor.setBookmarked(entry, chapters, bookmarked)
    }
}

internal class ProviderBackedEntryProgressInteraction(
    private val processors: Map<EntryType, EntryProgressProcessor>,
) : EntryProgressInteraction {
    override suspend fun snapshot(entry: Entry): EntryProgressSnapshot {
        val processor = processors.requireProcessor("progress", entry.type)
        return processor.snapshot(entry)
    }

    override suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot) {
        val processor = processors.requireProcessor("progress", entry.type)
        processor.restore(entry, snapshot)
    }

    override suspend fun copy(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ) {
        require(sourceEntry.type == targetEntry.type) {
            "Progress copy requires matching Entry types, but source was ${sourceEntry.type} and target was " +
                targetEntry.type
        }
        val processor = processors.requireProcessor("progress", sourceEntry.type)
        processor.copy(sourceEntry, targetEntry, resourceMappings)
    }

    override suspend fun prepareMigration(
        sourceEntry: Entry,
        targetEntry: Entry,
        resourceMappings: List<EntryProgressResourceMapping>,
    ): EntryProgressSnapshot {
        require(sourceEntry.type == targetEntry.type) {
            "Progress Migration requires matching Entry types, but source was ${sourceEntry.type} and target was " +
                targetEntry.type
        }
        val processor = processors.requireProcessor("progress", sourceEntry.type)
        return processor.prepareMigration(sourceEntry, targetEntry, resourceMappings)
    }
}

internal class ProviderBackedEntryPlaybackPreferencesInteraction(
    private val processors: Map<EntryType, EntryPlaybackPreferencesProcessor>,
) : EntryPlaybackPreferencesInteraction {
    override suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot? {
        val processor = processors.requireProcessor("playback preferences", entry.type)
        return processor.snapshot(entry)
    }

    override suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot) {
        val processor = processors.requireProcessor("playback preferences", entry.type)
        processor.restore(entry, snapshot)
    }

    override suspend fun copy(sourceEntry: Entry, targetEntry: Entry): Boolean {
        require(sourceEntry.type == targetEntry.type) {
            "Playback-preference copy requires matching Entry types, but received " +
                "${sourceEntry.type} and ${targetEntry.type}"
        }
        val processor = processors.requireProcessor("playback preferences", sourceEntry.type)
        return processor.copy(sourceEntry, targetEntry)
    }
}
