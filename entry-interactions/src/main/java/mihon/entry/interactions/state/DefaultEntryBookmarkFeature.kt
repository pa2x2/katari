package mihon.entry.interactions.state

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryBookmarkFeature(
    private val applicableTypes: Set<EntryType>,
    private val interaction: EntryBookmarkInteraction,
) : EntryBookmarkFeature {
    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override fun availability(
        target: EntryBookmarkTarget,
        bookmarked: Boolean,
    ): EntryBookmarkAvailability {
        return selectionAvailability(listOf(target), bookmarked)
    }

    override fun selectionAvailability(
        targets: List<EntryBookmarkTarget>,
        bookmarked: Boolean,
    ): EntryBookmarkAvailability {
        val unsupportedTypes = targets.mapTo(mutableSetOf(), EntryBookmarkTarget::type) - applicableTypes
        if (unsupportedTypes.isNotEmpty()) return EntryBookmarkAvailability.Inapplicable(unsupportedTypes)
        if (targets.isEmpty()) return EntryBookmarkAvailability.NoChange

        val canChange = when (bookmarked) {
            true -> targets.any { !it.status.bookmarked }
            false -> targets.all { it.status.bookmarked }
        }
        return if (canChange) EntryBookmarkAvailability.Available else EntryBookmarkAvailability.NoChange
    }

    override suspend fun setBookmarked(
        entry: Entry,
        chapters: List<EntryChapter>,
        bookmarked: Boolean,
    ): EntryBookmarkMutationResult {
        if (!isApplicable(entry.type)) return EntryBookmarkMutationResult.Inapplicable(entry.type)
        val changedChapters = chapters.filter { it.bookmark != bookmarked }
        if (changedChapters.isEmpty()) return EntryBookmarkMutationResult.NoChange

        interaction.setBookmarked(entry, changedChapters, bookmarked)
        return EntryBookmarkMutationResult.Applied(changedChapters.size)
    }
}
