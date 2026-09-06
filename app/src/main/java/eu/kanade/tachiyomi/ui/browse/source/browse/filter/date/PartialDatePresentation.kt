package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.tachiyomi.source.entry.filter.EntryDatePrecision
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun datePickerLocale(): Locale = LocalConfiguration.current.locales[0]

internal fun EntryPartialDate.displayDate(locale: Locale): String {
    val skeleton = when (precision) {
        EntryDatePrecision.YEAR -> "y"
        EntryDatePrecision.MONTH -> "yMMM"
        EntryDatePrecision.DAY -> "yMMMd"
    }
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return LocalDate.of(year, month ?: 1, day ?: 1).format(DateTimeFormatter.ofPattern(pattern, locale))
}

@Composable
internal fun precisionLabel(precision: EntryDatePrecision): String = stringResource(
    when (precision) {
        EntryDatePrecision.YEAR -> MR.strings.filter_precision_year
        EntryDatePrecision.MONTH -> MR.strings.filter_precision_month
        EntryDatePrecision.DAY -> MR.strings.filter_precision_day
    },
)

@Composable
internal fun chooseComponentLabel(precision: EntryDatePrecision): String = stringResource(
    when (precision) {
        EntryDatePrecision.YEAR -> MR.strings.filter_choose_year
        EntryDatePrecision.MONTH -> MR.strings.filter_choose_month
        EntryDatePrecision.DAY -> MR.strings.filter_choose_day
    },
)
