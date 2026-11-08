package app.katari.entry.interactions.statistics

import app.katari.entry.interactions.runtime.EntryStatisticsProvider
import eu.kanade.tachiyomi.source.entry.EntryType

interface EntryStatisticsInteraction {
    val contributions: List<EntryStatisticsContribution>
}

internal class ProviderBackedEntryStatisticsInteraction(
    providers: Map<EntryType, EntryStatisticsProvider>,
) : EntryStatisticsInteraction {
    override val contributions = providers.map { (type, provider) ->
        EntryStatisticsContribution(
            type = type,
            accent = provider.accent,
            consumedUnitLabel = provider.consumedUnitLabel,
            consumedUnitName = provider.consumedUnitName,
            timePerItemLabel = provider.timePerItemLabel,
            consumedCountPlural = provider.consumedCountPlural,
            chapterRuntime = provider.chapterRuntime,
            itemNumberLabel = provider.itemNumberLabel,
            itemRangeLabel = provider.itemRangeLabel,
        )
    }
}
