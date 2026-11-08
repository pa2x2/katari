package mihon.entry.interactions.child

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import mihon.entry.interactions.runtime.EntryChildListProcessor
import mihon.entry.interactions.runtime.EntryChildProgressProcessor
import mihon.entry.interactions.runtime.EntryMissingChildGapProcessor
import mihon.entry.interactions.runtime.EntryScanlatorFilterProcessor
import mihon.entry.interactions.runtime.requireProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class ProviderBackedEntryChildListInteraction(
    private val processors: Map<EntryType, EntryChildListProcessor>,
) : EntryChildListInteraction {
    override fun sortedForReading(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        val processor = processors.requireProcessor("child list", entry.type)
        return processor.sortedForReading(entry, chapters, memberIds)
    }

    override fun sortedForDisplay(
        entry: Entry,
        chapters: List<EntryChapter>,
        memberIds: List<Long>,
    ): List<EntryChapter> {
        val processor = processors.requireProcessor("child list", entry.type)
        return processor.sortedForDisplay(entry, chapters, memberIds)
    }
}

internal class ProviderBackedEntryChildProgressInteraction(
    private val processors: Map<EntryType, EntryChildProgressProcessor>,
) : EntryChildProgressInteraction {
    override fun progressLabels(request: EntryChildProgressRequest): Flow<Map<Long, EntryChildProgressLabel>> {
        val processor = processors.requireProcessor("child progress", request.entry.type)
        return processor.progressLabels(request)
    }
}

internal class ProviderBackedEntryMissingChildGapInteraction(
    private val processors: Map<EntryType, EntryMissingChildGapProcessor>,
) : EntryMissingChildGapInteraction {
    override fun buildDisplayList(request: EntryChildListRequest): EntryChildListDisplay {
        val processor = processors.requireProcessor("missing child gaps", request.entry.type)
        return processor.buildDisplayList(request)
    }
}

internal class ProviderBackedEntryScanlatorFilterInteraction(
    private val processors: Map<EntryType, EntryScanlatorFilterProcessor>,
) : EntryScanlatorFilterInteraction {
    override fun scanlatorFor(entry: Entry, chapter: EntryChapter): String? {
        val processor = processors.requireProcessor("scanlator filter", entry.type)
        return processor.scanlatorFor(entry, chapter)
    }

    override fun normalizeScanlator(entry: Entry, scanlator: String): String? {
        val processor = processors.requireProcessor("scanlator filter", entry.type)
        return processor.normalizeScanlator(entry, scanlator)
    }
}
