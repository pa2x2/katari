package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsTopTitle
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsTopTitlesCard(
    titles: List<StatsTopTitle>,
    typesById: Map<EntryType, StatsType>,
    periodLabel: String,
    formatDuration: (Long) -> String,
    onTitleClick: (Long) -> Unit,
) {
    val visibleTitles = titles.take(5)
    StatisticsSectionCard(
        title = stringResource(MR.strings.statistics_top_titles),
        trailingText = periodLabel,
    ) {
        visibleTitles.forEachIndexed { index, title ->
            val type = typesById[title.type]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTitleClick(title.entryId) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    modifier = Modifier.width(30.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    type?.let {
                        Text(
                            text = stringResource(it.displayName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(formatDuration(title.durationMillis), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (index != visibleTitles.lastIndex) HorizontalDivider()
        }
    }
}
