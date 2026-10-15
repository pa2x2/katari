package mihon.entry.interactions.navigation

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.requireProcessor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

internal class ProviderBackedEntryOpenInteraction(
    private val processors: Map<EntryType, EntryOpenProcessor>,
) : EntryOpenInteraction {
    override fun open(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        val processor = processors.requireProcessor("open", entry.type)
        processor.open(context, entry, chapter, options)
    }

    override fun pendingIntent(context: Context, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) =
        processors.requireProcessor("open", entry.type).pendingIntent(context, entry, chapter, options)
}

internal class ProviderBackedEntryContinueInteraction(
    private val processors: Map<EntryType, EntryContinueProcessor>,
) : EntryContinueInteraction {
    override suspend fun continueEntry(context: Context, entry: Entry): EntryChapter? {
        val processor = processors.requireProcessor("continue", entry.type)
        val chapter = processor.findNext(entry)
        if (chapter != null) {
            processor.open(context, entry, chapter)
        }
        return chapter
    }

    override suspend fun findNext(entry: Entry): EntryChapter? {
        val processor = processors.requireProcessor("continue", entry.type)
        return processor.findNext(entry)
    }

    override suspend fun findNext(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryChapter? {
        val processor = processors.requireProcessor("continue", entry.type)
        return processor.findNext(entry, chapters, progressStates)
    }
}
