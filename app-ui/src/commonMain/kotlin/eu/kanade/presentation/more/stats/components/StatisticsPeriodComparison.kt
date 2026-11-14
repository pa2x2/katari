package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ArrowDownward
import app.katari.icons.materialsymbols.rounded.ArrowUpward
import app.katari.presentation.kit.content.InlineIcon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsRange
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * How the period's time compares with the period before it, such as "59% more than Oct 17 – Nov 15": a percentage,
 * or the difference itself when the period before had none.
 */
@Composable
internal fun StatisticsPeriodComparison(activity: StatsActivity, formatDuration: (Long) -> String) {
    val previousTotal = activity.previousTotalDurationMillis
    val previousWindow = activity.previousWindow
    if (previousTotal == null || previousWindow == null) {
        if (activity.window.range != StatsRange.ALL) {
            Text(text = stringResource(MR.strings.statistics_no_earlier_period), role = TextRole.Supporting)
        }
        return
    }
    val delta = activity.totalDurationMillis - previousTotal
    val change = if (previousTotal > 0L) {
        "${(abs(delta) * 100.0 / previousTotal).roundToLong()}%"
    } else {
        formatDuration(abs(delta))
    }
    val period = currentPlatformContext().formatStatisticsWindow(previousWindow)
    val tone = when {
        delta > 0L -> ContentTone.Primary
        delta < 0L -> ContentTone.Error
        else -> ContentTone.Secondary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (delta != 0L) {
            InlineIcon(
                imageVector = if (delta >
                    0L
                ) {
                    MaterialSymbols.Rounded.ArrowUpward
                } else {
                    MaterialSymbols.Rounded.ArrowDownward
                },
                contentDescription = null,
                role = TextRole.Supporting,
                tone = tone,
            )
        }
        Text(
            text = when {
                delta > 0L -> stringResource(MR.strings.statistics_more_than_period, change, period)
                delta < 0L -> stringResource(MR.strings.statistics_less_than_period, change, period)
                else -> stringResource(MR.strings.statistics_same_as_period, period)
            },
            role = TextRole.Supporting,
            tone = tone,
        )
    }
}
