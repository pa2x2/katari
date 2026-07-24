package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class DefaultEntryBookmarkFeature(
    private val evaluation: FeatureGraphEvaluation,
    private val interaction: EntryBookmarkInteraction,
) : EntryBookmarkFeature {
    private val applicableTypes = evaluation.bookmarkTypes()

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
        targets.map(EntryBookmarkTarget::type).distinct().forEach { type ->
            evaluation.requireBookmarkAvailabilityContext(type, canChange)
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
        evaluation.requireBookmarkMutationContext(entry.type, changedChapters.isNotEmpty())
        if (changedChapters.isEmpty()) return EntryBookmarkMutationResult.NoChange

        interaction.setBookmarked(entry, changedChapters, bookmarked)
        return EntryBookmarkMutationResult.Applied(changedChapters.size)
    }
}
