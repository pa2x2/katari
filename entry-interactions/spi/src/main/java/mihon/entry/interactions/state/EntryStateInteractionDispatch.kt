package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
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
        processor.requireMatchingEntryType("consumption", entry, consumptionProcessors.keys)
        return processor.setConsumed(entry, chapters, consumed)
    }
}

internal class ProviderBackedEntryBookmarkInteraction(
    private val bookmarkProcessors: Map<EntryType, EntryBookmarkProcessor>,
) : EntryBookmarkInteraction {
    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        val processor = bookmarkProcessors.requireProcessor("bookmark", entry.type)
        processor.requireMatchingEntryType("bookmark", entry, bookmarkProcessors.keys)
        processor.setBookmarked(entry, chapters, bookmarked)
    }
}

internal class ProviderBackedEntryProgressInteraction(
    private val processors: Map<EntryType, EntryProgressProcessor>,
) : EntryProgressInteraction {
    override suspend fun snapshot(entry: Entry): EntryProgressSnapshot {
        val processor = processors.requireProcessor("progress", entry.type)
        processor.requireMatchingEntryType("progress", entry, processors.keys)
        return processor.snapshot(entry)
    }

    override suspend fun restore(entry: Entry, snapshot: EntryProgressSnapshot) {
        val processor = processors.requireProcessor("progress", entry.type)
        processor.requireMatchingEntryType("progress", entry, processors.keys)
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
        processor.requireMatchingEntryType("progress", sourceEntry, processors.keys)
        processor.requireMatchingEntryType("progress", targetEntry, processors.keys)
        processor.copy(sourceEntry, targetEntry, resourceMappings)
    }
}

internal class ProviderBackedEntryPlaybackPreferencesInteraction(
    private val processors: Map<EntryType, EntryPlaybackPreferencesProcessor>,
) : EntryPlaybackPreferencesInteraction {
    override suspend fun snapshot(entry: Entry): EntryPlaybackPreferencesSnapshot? {
        val processor = processors.requireProcessor("playback preferences", entry.type)
        processor.requireMatchingEntryType("playback preferences", entry, processors.keys)
        return processor.snapshot(entry)
    }

    override suspend fun restore(entry: Entry, snapshot: EntryPlaybackPreferencesSnapshot) {
        val processor = processors.requireProcessor("playback preferences", entry.type)
        processor.requireMatchingEntryType("playback preferences", entry, processors.keys)
        processor.restore(entry, snapshot)
    }

    override suspend fun copy(sourceEntry: Entry, targetEntry: Entry): Boolean {
        require(sourceEntry.type == targetEntry.type) {
            "Playback-preference copy requires matching Entry types, but received " +
                "${sourceEntry.type} and ${targetEntry.type}"
        }
        val processor = processors.requireProcessor("playback preferences", sourceEntry.type)
        processor.requireMatchingEntryType("playback preferences", sourceEntry, processors.keys)
        processor.requireMatchingEntryType("playback preferences", targetEntry, processors.keys)
        return processor.copy(sourceEntry, targetEntry)
    }
}
