package eu.kanade.presentation.history.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.RowGroupSummaryHeader
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.domain.ui.bestDateTimePattern
import eu.kanade.presentation.util.rememberTimeFormatter
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDay
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A day above its card: its date and total, then its hours ribbon and span. [showsSessions] is null when the day
 * can't be opened up into its sessions, and otherwise says whether it is.
 */
@Composable
internal fun ActivityDayHeader(
    day: HistoryActivityDay,
    colors: Map<EntryType, Color>,
    formatDuration: (Long) -> String,
    showsSessions: Boolean?,
    onToggleSessions: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val withYear = day.localDate.year != LocalDate.now().year
    val dateFormatter = remember(locale, withYear) {
        DateTimeFormatter.ofPattern(bestDateTimePattern(locale, if (withYear) "yEEEMMMd" else "EEEMMMd"), locale)
    }
    val timeFormatter = rememberTimeFormatter()
    RowGroupSummaryHeader(
        title = day.localDate.format(dateFormatter),
        value = formatDuration(day.durationMillis),
        onClickLabel = showsSessions?.let {
            stringResource(if (it) MR.strings.statistics_show_day_totals else MR.strings.statistics_show_sessions)
        },
        onClick = onToggleSessions.takeIf { showsSessions != null },
    ) {
        ActivityHoursRibbon(day.spans, colors, Modifier.weight(1f))
        Text(
            text = "${day.startedAt.format(timeFormatter)} – ${day.endedAt.format(timeFormatter)}",
            role = TextRole.Supporting,
            maxLines = 1,
        )
    }
}
