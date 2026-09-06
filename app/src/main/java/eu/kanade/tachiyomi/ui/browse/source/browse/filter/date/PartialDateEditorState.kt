package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import java.time.Year

/** Incomplete choices and browsing position are editor state, never source values. */
internal data class PartialDateEditorState(
    val precision: EntryDatePrecision,
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val step: EntryDatePrecision = EntryDatePrecision.YEAR,
    val browsingYear: Int,
    val typing: Boolean = false,
    val raw: String = "",
    val showErrors: Boolean = false,
) {
    val value: EntryPartialDate?
        get() = if (typing) {
            EntryPartialDate.parse(raw)
        } else {
            when {
                year == null -> null
                precision == EntryDatePrecision.YEAR -> EntryPartialDate(year)
                month == null -> null
                precision == EntryDatePrecision.MONTH -> EntryPartialDate(year, month)
                day == null -> null
                else -> EntryPartialDate(year, month, day)
            }
        }

    val candidateText: String get() = if (typing) raw else value?.toString().orEmpty()
    val yearPageStart: Int get() = (browsingYear - 1) / 12 * 12 + 1

    fun missingStep(): EntryDatePrecision? = when {
        year == null -> EntryDatePrecision.YEAR
        precision != EntryDatePrecision.YEAR && month == null -> EntryDatePrecision.MONTH
        precision == EntryDatePrecision.DAY && day == null -> EntryDatePrecision.DAY
        else -> null
    }

    fun choosePrecision(next: EntryDatePrecision): PartialDateEditorState {
        val selected = if (typing) fromParsedInput() else this
        return selected.copy(precision = next, typing = false, showErrors = false).advance()
    }

    fun chooseYear(next: Int): PartialDateEditorState = copy(
        year = next,
        browsingYear = next,
        day = day?.takeIf { month != null && it <= EntryPartialDate.daysInMonth(next, month) },
    ).advance()

    fun chooseMonth(next: Int): PartialDateEditorState = copy(
        month = next,
        day = day?.takeIf { year != null && it <= EntryPartialDate.daysInMonth(year, next) },
    ).advance()

    fun chooseDay(next: Int): PartialDateEditorState = copy(day = next).advance()

    fun browseYears(delta: Int): PartialDateEditorState = copy(browsingYear = (yearPageStart + delta).coerceIn(1, 9999))

    fun editComponent(component: EntryDatePrecision): PartialDateEditorState = copy(
        step = component,
        typing = false,
        browsingYear = year ?: browsingYear,
    )

    fun toggleTyping(allowed: Set<EntryDatePrecision>): PartialDateEditorState = if (typing) {
        val selected = fromParsedInput()
        selected.copy(
            precision = EntryPartialDate.parse(raw)?.precision?.takeIf { it in allowed } ?: precision,
            typing = false,
            showErrors = false,
        ).advance()
    } else {
        copy(typing = true, raw = value?.toString() ?: raw, showErrors = false)
    }

    private fun fromParsedInput(): PartialDateEditorState = EntryPartialDate.parse(raw)?.let {
        copy(year = it.year, month = it.month, day = it.day, browsingYear = it.year)
    } ?: this

    private fun advance(): PartialDateEditorState = copy(step = missingStep() ?: precision, showErrors = false)

    companion object {
        fun initial(filter: EntryDateFilter, currentYear: Int = Year.now().value): PartialDateEditorState {
            val date = EntryPartialDate.parse(filter.state)
            val precision = date?.precision?.takeIf { it in filter.allowedPrecisions }
                ?: EntryDatePrecision.entries.first { it in filter.allowedPrecisions }
            return PartialDateEditorState(
                precision = precision,
                year = date?.year,
                month = date?.month,
                day = date?.day,
                browsingYear = date?.year ?: currentYear.coerceIn(
                    filter.minimum?.year ?: 1,
                    filter.maximum?.year ?: 9999,
                ),
                typing = filter.state.isNotBlank() && (date == null || date.precision !in filter.allowedPrecisions),
                raw = filter.state,
                showErrors = filter.state.isNotBlank(),
            ).let { it.copy(step = it.missingStep() ?: precision) }
        }
    }
}
