package mihon.entry.interactions.navigation

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.core.platform.PlatformContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryOpenFeature(
    private val applicableTypes: Set<EntryType>,
    private val interaction: EntryOpenInteraction,
) : EntryOpenFeature {
    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override fun open(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): Boolean {
        if (!isApplicable(entry.type)) return false
        interaction.open(context, entry, chapter, options)
        return true
    }
}
