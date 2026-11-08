package mihon.entry.interactions.navigation

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.core.platform.PlatformContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Feature-owned gate for every shared way of opening an Entry child. */
interface EntryOpenFeature {
    /**
     * Whether children of [type] are opened, so screens offer to open them. Also true where this platform doesn't have
     * the type's viewer yet: opening then answers [EntryOpenResult.ViewerUnavailable].
     */
    fun isApplicable(type: EntryType): Boolean

    fun open(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions = EntryOpenOptions(),
    ): EntryOpenResult
}

sealed interface EntryOpenResult {
    data object Opened : EntryOpenResult

    data object Inapplicable : EntryOpenResult

    /** The type is opened in a reader or player that this platform doesn't have yet. */
    data object ViewerUnavailable : EntryOpenResult
}
