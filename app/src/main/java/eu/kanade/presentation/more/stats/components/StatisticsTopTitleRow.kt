package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt
import tachiyomi.domain.entry.model.EntryCover as EntryCoverData

/**
 * One ranked title: cover, optional type label, completed children, time and its share of [shareOf].
 * Shared by the dashboard card, the See-all list and the Lifetime breakdown.
 */
@Composable
internal fun StatisticsTopTitleRow(
    rank: Int?,
    title: String,
    cover: EntryCoverData,
    type: EntryType,
    typeLabel: String?,
    completionCount: Long,
    durationMillis: Long,
    shareOf: Long,
    color: Color,
    formatDuration: (Long) -> String,
    onClick: () -> Unit,
) {
    val share = if (shareOf > 0L) durationMillis.toFloat() / shareOf else 0f
    val details = listOfNotNull(
        typeLabel,
        completionCount.takeIf { it > 0L }?.let { count ->
            pluralStringResource(type.entryTypePresentation().childCountPlural, count.toInt(), count.toInt())
        },
    ).joinToString(" · ")
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        rank?.let {
            Text(
                text = it.toString(),
                modifier = Modifier.width(24.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        EntryCover.Book(data = cover, modifier = Modifier.width(40.dp), shape = MaterialTheme.shapes.extraSmall)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { share.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        Spacer(Modifier.width(12.dp))
        // A fixed width keeps every share bar on the same scale regardless of the duration text.
        Column(Modifier.widthIn(min = TRAILING_COLUMN_WIDTH), horizontalAlignment = Alignment.End) {
            Text(formatDuration(durationMillis), style = MaterialTheme.typography.labelLarge)
            if (shareOf > 0L) {
                val percent = (share * 100).roundToInt()
                Text(
                    text = if (percent == 0 && durationMillis > 0L) {
                        stringResource(MR.strings.statistics_share_below_percent, 1)
                    } else {
                        stringResource(MR.strings.statistics_share_of_time, percent)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val TRAILING_COLUMN_WIDTH = 64.dp
