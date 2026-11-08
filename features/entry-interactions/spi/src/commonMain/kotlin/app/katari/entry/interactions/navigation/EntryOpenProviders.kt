package app.katari.entry.interactions.navigation

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState

/** Opens a child in the type's viewer, its reader or player, on a platform that has it. */
@PlatformFacility
interface EntryOpenProcessor {
    fun open(context: PlatformContext, entry: Entry, chapter: EntryChapter, options: EntryOpenOptions)
}

/** Picks the child that continuing an entry opens. The pick needs no viewer, so a type contributes it everywhere. */
interface EntryContinueProcessor {
    /**
     * Picks the chapter to continue with. [chapters] are only the ones the entry's list shows, across its merge members
     * and in no particular order. [progressStates] can still hold progress of other chapters, which must not be picked.
     */
    fun findNext(entry: Entry, chapters: List<EntryChapter>, progressStates: List<EntryProgressState>): EntryChapter?
}

/**
 * Stands in for a type's viewer on a platform that doesn't have it yet, under the type's key in place of its
 * [EntryOpenProcessor]. The type's children stay openable there, and opening one answers that the platform has no
 * viewer for it. A type with neither can't be opened at all.
 */
@PlatformFacility
data object EntryViewerStandIn
