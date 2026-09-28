package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Everything ever recorded: the detailed all-time total plus undated time from reading history. Only shown in
 * the All range, where [trackedDurationMillis] already covers every tracked day.
 */
@Composable
internal fun StatisticsLifetimeCard(
    trackedDurationMillis: Long,
    earlierDurationMillis: Long,
    trackingStartDate: LocalDate?,
    color: Color,
    formatDuration: (Long) -> String,
    onOpenEarlier: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    val total = trackedDurationMillis + earlierDurationMillis
    val earlierColor = MaterialTheme.colorScheme.outline
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_lifetime), contentSpacing = 8.dp) {
        Text(
            text = formatDuration(total),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (total > 0L) {
            val trackedShare = trackedDurationMillis.toFloat() / total
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
            ) {
                val trackedWidth = size.width * trackedShare
                drawRect(color, size = Size(trackedWidth, size.height))
                drawRect(
                    earlierColor.copy(alpha = 0.35f),
                    topLeft = Offset(trackedWidth, 0f),
                    size = Size(size.width - trackedWidth, size.height),
                )
                drawUntrackedHatch(trackedWidth, size.width, 0f, size.height, earlierColor)
            }
        }
        LifetimeRow(
            swatch = color,
            label = stringResource(MR.strings.statistics_tracked_in_detail),
            detail = trackingStartDate?.let {
                stringResource(MR.strings.statistics_tracking_since, it.format(dateFormatter))
            },
            duration = formatDuration(trackedDurationMillis),
            onClick = null,
        )
        if (earlierDurationMillis > 0L) {
            LifetimeRow(
                swatch = earlierColor,
                label = stringResource(MR.strings.statistics_earlier_from_history),
                detail = stringResource(MR.strings.statistics_earlier_without_dates),
                duration = formatDuration(earlierDurationMillis),
                onClick = onOpenEarlier,
            )
        }
    }
}

@Composable
private fun LifetimeRow(
    swatch: Color,
    label: String,
    detail: String?,
    duration: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(10.dp).clip(CircleShape)) { drawRect(swatch) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(duration, style = MaterialTheme.typography.labelLarge)
        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
