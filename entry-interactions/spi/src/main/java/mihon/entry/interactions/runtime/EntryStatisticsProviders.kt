package mihon.entry.interactions.runtime

import dev.icerock.moko.resources.StringResource
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import mihon.feature.graph.CapabilityId

interface EntryStatisticsProvider : EntryInteractionProvider {
    val accent: EntryStatisticsAccent
    val consumedUnitLabel: StringResource

    /**
     * Whether an item of one title takes about as long as an item of another, so a title without enough timed items
     * of its own can be estimated from the type's usual pace. False where item length varies widely between titles.
     */
    val itemPaceCarriesAcrossTitles: Boolean
}

val EntryStatisticsCapability = entryInteractionCapability<EntryStatisticsProvider>(
    id = CapabilityId("entry.statistics"),
)
