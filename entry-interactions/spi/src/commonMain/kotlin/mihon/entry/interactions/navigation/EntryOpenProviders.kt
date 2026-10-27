package mihon.entry.interactions.navigation

import mihon.core.platform.PlatformContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

interface EntryOpenProcessor {
    fun open(context: PlatformContext, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions)
}

interface EntryContinueProcessor {
    /**
     * Picks the chapter to continue with. [chapters] are only the ones the entry's list shows, across its merge members
     * and in no particular order. [progressStates] can still hold progress of other chapters, which must not be picked.
     */
    fun findNext(entry: Entry, chapters: List<EntryChapter>, progressStates: List<EntryProgressState>): EntryChapter?
    fun open(context: PlatformContext, entry: Entry, chapter: EntryChapter)
}
