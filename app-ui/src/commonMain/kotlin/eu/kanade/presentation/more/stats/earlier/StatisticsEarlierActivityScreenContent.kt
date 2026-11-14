package eu.kanade.presentation.more.stats.earlier

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import app.katari.presentation.kit.content.LeadFigure
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.surface.DashboardCard
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.stats.components.StatisticsTopTitleRow
import eu.kanade.presentation.more.stats.components.StatisticsTypeSplit
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.components.statisticsTypeShares
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.earlier.StatisticsEarlierActivityViewModel
import tachiyomi.domain.statistics.model.StatisticsEarlierActivityDetails
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun StatisticsEarlierActivityScreenContent(
    state: StatisticsEarlierActivityViewModel.State,
    selectedType: EntryType?,
    types: List<StatsType>,
    paddingValues: PaddingValues,
    onEntryClick: (Long) -> Unit,
) {
    when (state) {
        StatisticsEarlierActivityViewModel.State.Loading -> LoadingScreen(Modifier.padding(paddingValues))
        StatisticsEarlierActivityViewModel.State.Failed -> EmptyScreen(
            stringRes = MR.strings.statistics_could_not_load_activity,
            modifier = Modifier.padding(paddingValues),
        )
        is StatisticsEarlierActivityViewModel.State.Success -> {
            val details = state.details
            val totalDuration = details.totals.sumOf { it.durationMillis }
            if (totalDuration <= 0L) {
                EmptyScreen(stringRes = MR.strings.statistics_no_activity, modifier = Modifier.padding(paddingValues))
            } else {
                EarlierActivityList(details, totalDuration, selectedType, types, paddingValues, onEntryClick)
            }
        }
    }
}

@Composable
private fun EarlierActivityList(
    details: StatisticsEarlierActivityDetails,
    totalDuration: Long,
    selectedType: EntryType?,
    types: List<StatsType>,
    paddingValues: PaddingValues,
    onEntryClick: (Long) -> Unit,
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val typesById = types.associateBy(StatsType::type)
    RowList(
        contentPadding = paddingValues + PaddingValues(top = KatariTheme.spacing.m, bottom = KatariTheme.spacing.xl),
    ) {
        item("summary") {
            DashboardCard {
                LeadFigure(
                    value = formatDuration(totalDuration),
                    detail = {
                        Text(
                            text = earlierExplanation(details.trackingStartedAtEpochMillis),
                            modifier = Modifier.padding(top = KatariTheme.spacing.xs),
                            role = TextRole.Supporting,
                            tone = ContentTone.Secondary,
                        )
                    },
                )
                if (selectedType == null) {
                    StatisticsTypeSplit(
                        shares = statisticsTypeShares(
                            durationByType = details.totals.associate { it.type to it.durationMillis },
                            types = types,
                            typeLabels = types.associate { it.type to stringResource(it.displayName) },
                            typeColors = types.associate { it.type to it.accent.color() },
                            formatDuration = formatDuration,
                        ),
                        modifier = Modifier.padding(top = KatariTheme.spacing.m),
                    )
                }
            }
        }
        if (details.topEntries.isNotEmpty()) {
            item("titles-header") { RowGroupHeader(stringResource(MR.strings.statistics_top_titles)) }
        }
        itemsIndexed(details.topEntries, key = { _, entry -> entry.entryId }) { index, entry ->
            RowGroupItem(position = RowGroupPosition.of(index, details.topEntries.size)) {
                StatisticsTopTitleRow(
                    rank = index + 1,
                    title = entry.title,
                    cover = entry.cover,
                    type = entry.type,
                    completionCount = 0L,
                    durationMillis = entry.durationMillis,
                    shareOf = totalDuration,
                    color = typesById[entry.type]?.accent?.color() ?: KatariTheme.chart.series,
                    formatDuration = formatDuration,
                    onClick = { onEntryClick(entry.entryId) },
                    inRowGroup = true,
                )
            }
        }
    }
}

@Composable
private fun earlierExplanation(trackingStartedAtEpochMillis: Long?): String {
    val locale = LocalLocale.current.platformLocale
    val trackingStartDate = remember(trackingStartedAtEpochMillis, locale) {
        trackingStartedAtEpochMillis?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        }
    }
    return trackingStartDate?.let { stringResource(MR.strings.statistics_earlier_explanation, it) }
        ?: stringResource(MR.strings.statistics_earlier_explanation_untracked)
}
