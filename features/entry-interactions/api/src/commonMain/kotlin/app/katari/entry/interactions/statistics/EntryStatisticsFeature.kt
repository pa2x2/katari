package app.katari.entry.interactions.statistics

import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType

/** Host-controlled accent choices that keep contributed Statistics colors themeable and contrast-safe. */
enum class EntryStatisticsAccent {
    ROSE,
    SKY,
    SAGE,
    PLUM,
    AMBER,
}

data class EntryStatisticsContribution(
    val type: EntryType,
    val accent: EntryStatisticsAccent,
    val consumedUnitLabel: StringResource,
    val consumedUnitName: StringResource,
    val timePerItemLabel: StringResource,
    val consumedCountPlural: PluralsResource,
    /** Null for types whose progress doesn't record time. */
    val chapterRuntime: EntryChapterRuntimeReader?,
    val itemNumberLabel: StringResource,
    val itemRangeLabel: StringResource,
)

/** Compile-time entry-type contributions used to assemble Statistics without a closed app-level type list. */
interface EntryStatisticsFeature {
    val contributions: List<EntryStatisticsContribution>

    fun contribution(type: EntryType): EntryStatisticsContribution?
}
