package mihon.entry.interactions.runtime

import dev.icerock.moko.resources.StringResource
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import mihon.feature.graph.CapabilityId

interface EntryStatisticsProvider : EntryInteractionProvider {
    val accent: EntryStatisticsAccent
    val consumedUnitLabel: StringResource
}

val EntryStatisticsCapability = entryInteractionCapability<EntryStatisticsProvider>(
    id = CapabilityId("entry.statistics"),
)
