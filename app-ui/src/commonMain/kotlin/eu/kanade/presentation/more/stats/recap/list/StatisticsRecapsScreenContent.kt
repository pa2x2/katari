package eu.kanade.presentation.more.stats.recap.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.AutoStories
import app.katari.presentation.kit.content.Badge
import app.katari.presentation.kit.content.BadgeTone
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.rowGroupItems
import eu.kanade.tachiyomi.ui.stats.recap.list.StatisticsRecapsViewModel
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.recapPeriodTitle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun StatisticsRecapsScreenContent(
    state: StatisticsRecapsViewModel.State,
    paddingValues: PaddingValues,
    onOpen: (StatisticsRecapPeriod) -> Unit,
) {
    if (state.years.isEmpty() && state.months.isEmpty()) {
        EmptyScreen(MR.strings.statistics_recaps_empty, Modifier.padding(paddingValues))
        return
    }
    RowList(contentPadding = paddingValues) {
        if (state.years.isNotEmpty()) {
            item(key = "years") { RowGroupHeader(stringResource(MR.strings.statistics_recaps_years)) }
            rowGroupItems(state.years, key = { "year-${it.year}" }) { year ->
                RecapRow(
                    title = recapPeriodTitle(year),
                    isNew = year == state.unopened,
                    onClick = { onOpen(year) },
                )
            }
        }
        if (state.months.isNotEmpty()) {
            item(key = "months") { RowGroupHeader(stringResource(MR.strings.statistics_recaps_months)) }
            rowGroupItems(state.months, key = { "month-${it.month}" }) { month ->
                RecapRow(title = recapPeriodTitle(month), isNew = false, onClick = { onOpen(month) })
            }
        }
    }
}

@Composable
private fun RecapRow(title: String, isNew: Boolean, onClick: () -> Unit) {
    SettingsRow(
        title = title,
        icon = MaterialSymbols.Rounded.AutoStories,
        trailing = if (isNew) {
            { Badge(text = stringResource(MR.strings.statistics_recap_new), tone = BadgeTone.Primary) }
        } else {
            null
        },
        onClick = onClick,
    )
}
