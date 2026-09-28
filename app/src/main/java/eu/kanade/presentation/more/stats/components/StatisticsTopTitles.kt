package eu.kanade.presentation.more.stats.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsTopTitle
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The period's five most-used titles; the type label is only needed when several types are mixed. */
@Composable
internal fun StatisticsTopTitlesCard(
    titles: List<StatsTopTitle>,
    totalDurationMillis: Long,
    typesById: Map<EntryType, StatsType>,
    showType: Boolean,
    formatDuration: (Long) -> String,
    onTitleClick: (Long) -> Unit,
    onSeeAll: () -> Unit,
) {
    StatisticsSectionCard(
        title = stringResource(MR.strings.statistics_top_titles),
        // The snapshot only ranks the top five, so a full list is the signal that more titles may exist.
        actionLabel = stringResource(MR.strings.statistics_see_all).takeIf { titles.size >= DASHBOARD_TITLE_COUNT },
        onActionClick = onSeeAll,
        contentSpacing = 8.dp,
    ) {
        titles.take(DASHBOARD_TITLE_COUNT).forEachIndexed { index, title ->
            val type = typesById[title.type]
            StatisticsTopTitleRow(
                rank = index + 1,
                title = title.title,
                cover = title.cover,
                type = title.type,
                typeLabel = type?.takeIf { showType }?.let { stringResource(it.displayName) },
                completionCount = title.completionCount,
                durationMillis = title.durationMillis,
                shareOf = totalDurationMillis,
                color = type?.accent?.color() ?: MaterialTheme.colorScheme.primary,
                formatDuration = formatDuration,
                onClick = { onTitleClick(title.entryId) },
            )
        }
    }
}

internal const val DASHBOARD_TITLE_COUNT = 5
