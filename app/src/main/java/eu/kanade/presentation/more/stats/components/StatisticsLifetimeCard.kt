package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ChevronRight
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LegendSwatch
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.chartBar
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
    val earlierColor = KatariTheme.chart.untracked
    val earlierFill = KatariTheme.chart.untrackedFill
    val hatchStroke = KatariTheme.sizes.border
    StatisticsSectionCard(
        title = stringResource(MR.strings.statistics_lifetime),
        contentSpacing = KatariTheme.spacing.s,
    ) {
        Text(text = formatDuration(total), role = TextRole.Metric)
        if (total > 0L) {
            val trackedShare = trackedDurationMillis.toFloat() / total
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .padding(top = KatariTheme.spacing.m, bottom = KatariTheme.spacing.xs)
                    .height(StatisticsMetrics.stageBar)
                    .chartBar(),
            ) {
                val trackedWidth = size.width * trackedShare
                drawRect(color, size = Size(trackedWidth, size.height))
                drawRect(
                    earlierFill,
                    topLeft = Offset(trackedWidth, 0f),
                    size = Size(size.width - trackedWidth, size.height),
                )
                drawUntrackedHatch(trackedWidth, size.width, 0f, size.height, earlierColor, hatchStroke)
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
            .padding(vertical = KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
    ) {
        LegendSwatch(swatch)
        Column(Modifier.weight(1f)) {
            Text(label, role = TextRole.Body)
            detail?.let { Text(text = it, role = TextRole.Supporting) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(duration, role = TextRole.Label)
            if (onClick != null) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.padding(start = KatariTheme.spacing.xs).size(KatariTheme.sizes.iconSmall),
                    tone = ContentTone.Secondary,
                )
            }
        }
    }
}
