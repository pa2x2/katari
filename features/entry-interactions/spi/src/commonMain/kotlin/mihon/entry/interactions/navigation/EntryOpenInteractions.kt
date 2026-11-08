package mihon.entry.interactions.navigation

import mihon.core.platform.PlatformContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

interface EntryOpenInteraction {
    fun open(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions = EntryOpenOptions(),
    )
}

interface EntryContinueInteraction {
    fun findNext(entry: Entry, chapters: List<EntryChapter>, progressStates: List<EntryProgressState>): EntryChapter?
}
