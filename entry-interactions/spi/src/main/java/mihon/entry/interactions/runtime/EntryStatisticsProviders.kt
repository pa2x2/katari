package mihon.entry.interactions.runtime

import mihon.entry.interactions.statistics.EntryStatisticsAccent
import mihon.feature.graph.CapabilityId

interface EntryStatisticsProvider : EntryInteractionProvider {
    val accent: EntryStatisticsAccent
}

val EntryStatisticsCapability = entryInteractionCapability<EntryStatisticsProvider>(
    id = CapabilityId("entry.statistics"),
)
