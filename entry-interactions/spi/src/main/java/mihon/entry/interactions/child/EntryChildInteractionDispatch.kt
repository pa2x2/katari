package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.sortedForReading

internal class ProviderBackedEntryChildListInteraction(
    private val processors: Map<EntryType, EntryChildListProcessor>,
) : EntryChildListInteraction {
    override fun sortedForReading(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        val processor = processors.requireProcessor("child list", entry.type)
        processor.requireMatchingEntryType("child list", entry, processors.keys)
        return processor.sortedForReading(entry, chapters, memberIds)
    }

    override fun sortedForDisplay(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        val processor = processors.requireProcessor("child list", entry.type)
        processor.requireMatchingEntryType("child list", entry, processors.keys)
        return processor.sortedForDisplay(entry, chapters, memberIds)
    }
}

internal class ProviderBackedEntryChildProgressInteraction(
    private val processors: Map<EntryType, EntryChildProgressProcessor>,
) : EntryChildProgressInteraction {
    override fun progressLabels(request: EntryChildProgressRequest): Flow<Map<Long, EntryChildProgressLabel>> {
        val processor = processors.requireProcessor("child progress", request.entry.type)
        processor.requireMatchingEntryType("child progress", request.entry, processors.keys)
        return processor.progressLabels(request)
    }
}

internal class ProviderBackedEntryMissingChildGapInteraction(
    private val processors: Map<EntryType, EntryMissingChildGapProcessor>,
) : EntryMissingChildGapInteraction {
    override fun buildDisplayList(request: EntryChildListRequest): EntryChildListDisplay {
        val processor = processors.requireProcessor("missing child gaps", request.entry.type)
        processor.requireMatchingEntryType("missing child gaps", request.entry, processors.keys)
        return processor.buildDisplayList(request)
    }
}

internal class ProviderBackedEntryChildGroupFilterInteraction(
    private val processors: Map<EntryType, EntryChildGroupFilterProcessor>,
) : EntryChildGroupFilterInteraction {
    override fun groupFor(entry: Entry, chapter: EntryChapter): String? {
        val processor = processors.requireProcessor("child group filter", entry.type)
        processor.requireMatchingEntryType("child group filter", entry, processors.keys)
        return processor.groupFor(entry, chapter)
    }

    override fun normalizeGroup(entry: Entry, group: String): String? {
        val processor = processors.requireProcessor("child group filter", entry.type)
        processor.requireMatchingEntryType("child group filter", entry, processors.keys)
        return processor.normalizeGroup(entry, group)
    }
}
