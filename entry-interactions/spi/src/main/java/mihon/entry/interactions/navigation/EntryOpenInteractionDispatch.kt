package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class ProviderBackedEntryOpenInteraction(
    private val processors: Map<EntryType, EntryOpenProcessor>,
) : EntryOpenInteraction {
    override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        val processor = processors.requireProcessor("open", entry.type)
        processor.requireMatchingEntryType("open", entry, processors.keys)
        processor.open(context, entry, chapter, options)
    }

    override fun pendingIntent(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) =
        processors.requireProcessor("open", entry.type).also {
            it.requireMatchingEntryType("open", entry, processors.keys)
        }.pendingIntent(context, entry, chapter, options)
}

internal class ProviderBackedEntryContinueInteraction(
    private val processors: Map<EntryType, EntryContinueProcessor>,
) : EntryContinueInteraction {
    override suspend fun continueEntry(context: Context, entry: Entry): EntryChapter? {
        val processor = processors.requireProcessor("continue", entry.type)
        processor.requireMatchingEntryType("continue", entry, processors.keys)
        val chapter = processor.findNext(entry)
        if (chapter != null) {
            processor.open(context, entry, chapter)
        }
        return chapter
    }

    override suspend fun findNext(entry: Entry): EntryChapter? {
        val processor = processors.requireProcessor("continue", entry.type)
        processor.requireMatchingEntryType("continue", entry, processors.keys)
        return processor.findNext(entry)
    }
}
