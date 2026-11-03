package eu.kanade.tachiyomi.ui.stats.recap.period

import eu.kanade.tachiyomi.source.entry.EntryType
import java.io.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** The days a recap covers, both ends included. Serializable so screens holding one survive process death. */
sealed interface StatisticsRecapPeriod : Serializable {
    val start: LocalDate
    val end: LocalDate

    /** A calendar year; [end] is before Dec 31 while the year is still going. */
    data class Year(val year: Int, override val end: LocalDate = LocalDate.of(year, 12, 31)) : StatisticsRecapPeriod {
        override val start: LocalDate get() = LocalDate.of(year, 1, 1)
        val isSoFar: Boolean get() = end.isBefore(LocalDate.of(year, 12, 31))
    }

    data class Month(val month: YearMonth) : StatisticsRecapPeriod {
        override val start: LocalDate get() = month.atDay(1)
        override val end: LocalDate get() = month.atEndOfMonth()
    }

    /**
     * Any window shown on the Statistics dashboard, shared as a summary card only.
     *
     * @param startDate null to start at the first recorded activity.
     * @param label how the window reads on the card, such as "Last 30 days".
     */
    data class Window(
        val startDate: LocalDate?,
        override val end: LocalDate,
        val type: EntryType?,
        val label: String,
    ) : StatisticsRecapPeriod {
        // Activity is recorded with epoch timestamps, so nothing predates 1970.
        override val start: LocalDate get() = startDate ?: LocalDate.of(1970, 1, 1)
    }
}

val StatisticsRecapPeriod.lengthInDays: Long get() = ChronoUnit.DAYS.between(start, end) + 1L

/**
 * The same span one year or month earlier, so a year still going is compared with the same days of the year before.
 * Null for windows, which have no natural predecessor.
 */
fun StatisticsRecapPeriod.previous(): StatisticsRecapPeriod? = when (this) {
    is StatisticsRecapPeriod.Year -> StatisticsRecapPeriod.Year(year - 1, end.minusYears(1L))
    is StatisticsRecapPeriod.Month -> StatisticsRecapPeriod.Month(month.minusMonths(1L))
    is StatisticsRecapPeriod.Window -> null
}
