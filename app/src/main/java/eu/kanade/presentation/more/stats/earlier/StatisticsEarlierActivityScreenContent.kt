package eu.kanade.presentation.more.stats.earlier

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.StatisticsSectionCard
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.earlier.StatisticsEarlierActivityScreenModel
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun StatisticsEarlierActivityScreenContent(
    state: StatisticsEarlierActivityScreenModel.State,
    selectedType: EntryType?,
    types: List<StatsType>,
    trackingStartedAtEpochMillis: Long?,
    paddingValues: PaddingValues,
    onEntryClick: (Long) -> Unit,
) {
    when (state) {
        StatisticsEarlierActivityScreenModel.State.Loading -> LoadingScreen(Modifier.padding(paddingValues))
        StatisticsEarlierActivityScreenModel.State.Failed -> CenteredMessage(
            paddingValues = paddingValues,
            message = stringResource(MR.strings.statistics_could_not_load_activity),
        )
        is StatisticsEarlierActivityScreenModel.State.Success -> {
            val formatDuration = rememberStatisticsDurationFormatter()
            val details = state.details
            val totalDuration = details.totals.sumOf { it.durationMillis }
            if (totalDuration <= 0L) {
                CenteredMessage(
                    paddingValues = paddingValues,
                    message = stringResource(MR.strings.statistics_no_activity),
                )
                return
            }
            val typesById = types.associateBy(StatsType::type)
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = paddingValues.calculateTopPadding() + 12.dp,
                    end = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                text = formatDuration(totalDuration),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            trackingStartedAtEpochMillis?.let { startedAt ->
                                Spacer(Modifier.height(4.dp))
                                val date = Instant.ofEpochMilli(startedAt)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                Text(
                                    text = stringResource(MR.strings.statistics_before_date, date),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                                    Text(
                                        formatDuration(total.durationMillis),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { total.durationMillis.toFloat() / maximum.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = type?.accent?.color() ?: MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Spacer(Modifier.height(14.dp))
                            }
                        }
                    }
                }
                if (details.topEntries.isNotEmpty()) {
                    item {
                        StatisticsSectionCard(stringResource(MR.strings.statistics_top_titles)) {
                            val maximum = details.topEntries.maxOf(StatisticsTopEntry::durationMillis).coerceAtLeast(1L)
                            details.topEntries.forEach { entry ->
                                val type = typesById[entry.type]
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onEntryClick(entry.entryId) },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (selectedType == null) {
                                            Text(
                                                text = type?.let { stringResource(it.displayName) } ?: entry.type.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        formatDuration(entry.durationMillis),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { entry.durationMillis.toFloat() / maximum.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = type?.accent?.color() ?: MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Spacer(Modifier.height(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(paddingValues: PaddingValues, message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
