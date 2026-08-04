package mihon.feature.upcoming

import kotlinx.datetime.LocalDate
import tachiyomi.domain.entry.model.Entry

sealed interface UpcomingUIModel {
    data class Header(val date: LocalDate, val entryCount: Int) : UpcomingUIModel
    data class Item(val entry: Entry) : UpcomingUIModel
}
