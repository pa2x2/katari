package mihon.feature.upcoming

import kotlinx.datetime.LocalDate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason

sealed interface UpcomingUIModel {
    data class Header(val date: LocalDate, val entryCount: Int) : UpcomingUIModel

    /** @param notCheckedReason why the next automatic update would leave the entry out, for reasons that last. */
    data class Item(val entry: Entry, val notCheckedReason: EntryUpdateDecisionReason? = null) : UpcomingUIModel
}
