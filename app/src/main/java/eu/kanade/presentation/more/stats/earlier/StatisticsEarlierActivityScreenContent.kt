package eu.kanade.presentation.more.stats.earlier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.presentation.more.stats.components.StatisticsSectionCard
import eu.kanade.presentation.more.stats.components.StatisticsShareBar
import eu.kanade.presentation.more.stats.components.StatisticsTopTitleRow
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.earlier.StatisticsEarlierActivityViewModel
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
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
            val formatDuration = rememberStatisticsDurationFormatter()
            val details = state.details
            val totalDuration = details.totals.sumOf { it.durationMillis }
            if (totalDuration <= 0L) {
                EmptyScreen(stringRes = MR.strings.statistics_no_activity, modifier = Modifier.padding(paddingValues))
                return
            }
            val typesById = types.associateBy(StatsType::type)
            val locale = LocalConfiguration.current.locales[0]
            val trackingStartDate = remember(details.trackingStartedAtEpochMillis, locale) {
                details.trackingStartedAtEpochMillis?.let { millis ->
                    Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(
                    start = KatariTheme.spacing.contentInset,
                    top = paddingValues.calculateTopPadding() + KatariTheme.spacing.m,
                    end = KatariTheme.spacing.contentInset,
                    bottom = paddingValues.calculateBottomPadding() + KatariTheme.spacing.xl,
                ),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            ) {
                item {
                    Card(Modifier.fillMaxWidth(), style = CardStyle.Outlined) {
                        Column(Modifier.padding(KatariTheme.spacing.contentInset)) {
                            Text(text = formatDuration(totalDuration), role = TextRole.Metric)
                            Spacer(Modifier.height(KatariTheme.spacing.s))
                            Text(
                                text = trackingStartDate?.let {
                                    stringResource(MR.strings.statistics_earlier_explanation, it)
                                } ?: stringResource(MR.strings.statistics_earlier_explanation_untracked),
                                role = TextRole.Body,
                                tone = ContentTone.Secondary,
                            )
                        }
                    }
                }
                if (selectedType == null && details.totals.size > 1) {
                    item {
                        StatisticsSectionCard(stringResource(MR.strings.statistics_by_media)) {
                            val maximum = details.totals.maxOf { it.durationMillis }.coerceAtLeast(1L)
                            details.totals.sortedByDescending { it.durationMillis }.forEach { total ->
                                val type = typesById[total.type]
                                val label = type?.let { stringResource(it.displayName) } ?: total.type.name
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(label)
                                    Text(formatDuration(total.durationMillis), role = TextRole.Label)
                                }
                                Spacer(Modifier.height(KatariTheme.spacing.s))
                                StatisticsShareBar(
                                    fraction = total.durationMillis.toFloat() / maximum.toFloat(),
                                    color = type?.accent?.color() ?: KatariTheme.colors.primary,
                                    height = StatisticsMetrics.shareBarThick,
                                )
                                Spacer(Modifier.height(KatariTheme.spacing.m))
                            }
                        }
                    }
                }
                if (details.topEntries.isNotEmpty()) {
                    item {
                        StatisticsSectionCard(
                            title = stringResource(MR.strings.statistics_top_titles),
                            contentSpacing = KatariTheme.spacing.s,
                        ) {
                            details.topEntries.forEachIndexed { index, entry ->
                                val type = typesById[entry.type]
                                StatisticsTopTitleRow(
                                    rank = index + 1,
                                    title = entry.title,
                                    cover = entry.cover,
                                    type = entry.type,
                                    typeLabel = type?.takeIf {
                                        selectedType == null
                                    }?.let { stringResource(it.displayName) },
                                    completionCount = 0L,
                                    durationMillis = entry.durationMillis,
                                    shareOf = totalDuration,
                                    color = type?.accent?.color() ?: KatariTheme.colors.primary,
                                    formatDuration = formatDuration,
                                    onClick = { onEntryClick(entry.entryId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
