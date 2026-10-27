package mihon.entry.interactions.navigation

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.core.platform.PlatformContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned gate for every shared way of opening an Entry child. */
interface EntryOpenFeature {
    fun isApplicable(type: EntryType): Boolean

    /** Opens [chapter] when the Entry type contributes Open behavior. */
    fun open(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions = EntryOpenOptions(),
    ): Boolean
}
