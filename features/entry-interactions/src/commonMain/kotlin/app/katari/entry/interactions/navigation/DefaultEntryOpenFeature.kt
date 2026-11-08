package app.katari.entry.interactions.navigation

import app.katari.core.platform.PlatformContext
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryOpenFeature(
    private val viewerTypes: Set<EntryType>,
    private val standInTypes: Set<EntryType>,
    private val interaction: EntryOpenInteraction,
) : EntryOpenFeature {
    init {
        require(viewerTypes.none(standInTypes::contains)) {
            "A viewer stand-in replaces a viewer this platform lacks, but ${viewerTypes intersect standInTypes} have both"
        }
    }

    override fun isApplicable(type: EntryType): Boolean = type in viewerTypes || type in standInTypes

    override fun open(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): EntryOpenResult = when (entry.type) {
        in viewerTypes -> {
            interaction.open(context, entry, chapter, options)
            EntryOpenResult.Opened
        }
        in standInTypes -> EntryOpenResult.ViewerUnavailable
        else -> EntryOpenResult.Inapplicable
    }
}
