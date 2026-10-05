package eu.kanade.presentation.more.stats.recap.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.stats.recap.list.StatisticsRecapsScreenModel
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.recapPeriodTitle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
fun StatisticsRecapsScreenContent(
    state: StatisticsRecapsScreenModel.State,
    paddingValues: PaddingValues,
    onOpen: (StatisticsRecapPeriod) -> Unit,
) {
    if (state.years.isEmpty() && state.months.isEmpty()) {
        EmptyScreen(MR.strings.statistics_recaps_empty, Modifier.padding(paddingValues))
        return
    }
    LazyColumn(contentPadding = paddingValues) {
        if (state.years.isNotEmpty()) {
            item(key = "years") { ListGroupHeader(stringResource(MR.strings.statistics_recaps_years)) }
            items(state.years, key = { "year-${it.year}" }) { year ->
                RecapRow(
                    title = recapPeriodTitle(year),
                    isNew = year == state.unopened,
                    onClick = { onOpen(year) },
                )
            }
        }
        if (state.months.isNotEmpty()) {
            item(key = "months") { ListGroupHeader(stringResource(MR.strings.statistics_recaps_months)) }
            items(state.months, key = { "month-${it.month}" }) { month ->
                RecapRow(title = recapPeriodTitle(month), isNew = false, onClick = { onOpen(month) })
            }
        }
    }
}

@Composable
private fun RecapRow(title: String, isNew: Boolean, onClick: () -> Unit) {
    ListItem(
        leadingContent = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
        trailingContent = if (isNew) {
            { Badge { Text(stringResource(MR.strings.statistics_recap_new)) } }
        } else {
            null
        },
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(title)
    }
}
