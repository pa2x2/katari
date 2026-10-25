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
    override fun findNext(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryChapter? = processors.requireProcessor("continue", entry.type).findNext(entry, chapters, progressStates)

    override fun open(context: Context, entry: Entry, chapter: EntryChapter) {
        processors.requireProcessor("continue", entry.type).open(context, entry, chapter)
    }
}
