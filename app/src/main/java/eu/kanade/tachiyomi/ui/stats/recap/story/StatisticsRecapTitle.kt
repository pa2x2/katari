package eu.kanade.tachiyomi.ui.stats.recap.story

import androidx.compose.runtime.Immutable
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.entry.model.EntryCover

/** A title as a recap shows it, with its time in the recap's period. */
@Immutable
data class StatisticsRecapTitle(
    val entryId: Long,
    val type: EntryType,
    val title: String,
    val cover: EntryCover,
    val durationMillis: Long,
)

/** Items finished in the period, worded by the types' plural, such as "2,140 chapters read". */
@Immutable
data class StatisticsRecapConsumedCount(val plural: PluralsResource, val count: Long)

/** One item, or a run of items, of an entry, worded by its type. */
@Immutable
sealed interface StatisticsRecapItems {
    data class Single(val label: StringResource, val number: Double) : StatisticsRecapItems

    data class Range(val label: StringResource, val first: Double, val last: Double) : StatisticsRecapItems
}
