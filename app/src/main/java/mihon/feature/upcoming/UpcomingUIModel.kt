package mihon.feature.upcoming

import tachiyomi.domain.entry.model.Entry
import java.time.LocalDate

sealed interface UpcomingUIModel {
    data class Header(val date: LocalDate, val entryCount: Int) : UpcomingUIModel
    data class Item(val entry: Entry) : UpcomingUIModel
}
