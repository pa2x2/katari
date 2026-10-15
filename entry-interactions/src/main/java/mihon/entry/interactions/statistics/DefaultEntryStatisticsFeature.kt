package mihon.entry.interactions.statistics

import eu.kanade.tachiyomi.source.entry.EntryType

internal class DefaultEntryStatisticsFeature(
    private val applicableTypes: Set<EntryType>,
    interaction: EntryStatisticsInteraction,
) : EntryStatisticsFeature {
    override val contributions = interaction.contributions
        .filter { it.type in applicableTypes }
        .sortedBy { EntryType.entries.indexOf(it.type) }
    private val contributionsByType = contributions.associateBy(EntryStatisticsContribution::type)

    override fun contribution(type: EntryType): EntryStatisticsContribution? = contributionsByType[type]
}
