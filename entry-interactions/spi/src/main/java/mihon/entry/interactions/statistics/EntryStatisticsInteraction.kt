package mihon.entry.interactions.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryStatisticsProvider

interface EntryStatisticsInteraction {
    val contributions: List<EntryStatisticsContribution>
}

internal class ProviderBackedEntryStatisticsInteraction(
    providers: Map<EntryType, EntryStatisticsProvider>,
) : EntryStatisticsInteraction {
    override val contributions = providers.values.map { provider ->
        EntryStatisticsContribution(
            type = provider.type,
            accent = provider.accent,
            consumedUnitLabel = provider.consumedUnitLabel,
            consumedCountPlural = provider.consumedCountPlural,
            itemPaceCarriesAcrossTitles = provider.itemPaceCarriesAcrossTitles,
            itemNumberLabel = provider.itemNumberLabel,
            itemRangeLabel = provider.itemRangeLabel,
        )
    }
}
