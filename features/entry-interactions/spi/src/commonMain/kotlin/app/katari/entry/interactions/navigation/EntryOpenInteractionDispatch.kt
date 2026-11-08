package app.katari.entry.interactions.navigation

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.runtime.requireProcessor
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

internal class ProviderBackedEntryOpenInteraction(
    private val processors: Map<EntryType, EntryOpenProcessor>,
) : EntryOpenInteraction {
    override fun open(context: PlatformContext, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions) {
        val processor = processors.requireProcessor("open", entry.type)
        processor.open(context, entry, chapter, options)
    }
}

internal class ProviderBackedEntryContinueInteraction(
    private val processors: Map<EntryType, EntryContinueProcessor>,
) : EntryContinueInteraction {
    override fun findNext(
        entry: Entry,
        chapters: List<EntryChapter>,
        progressStates: List<EntryProgressState>,
    ): EntryChapter? = processors.requireProcessor("continue", entry.type).findNext(entry, chapters, progressStates)
}
