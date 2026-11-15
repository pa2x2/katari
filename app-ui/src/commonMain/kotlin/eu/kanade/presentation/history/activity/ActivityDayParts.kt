package eu.kanade.presentation.history.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Check
import app.katari.presentation.kit.list.RowGroupSeparator
import app.katari.presentation.kit.list.TallyRow
import eu.kanade.presentation.util.rememberTimeFormatter
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDay
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityPart
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A day of one title's list: each chapter or episode once with the day's time, or, while [showsSessions], each session
 * with what it read.
 */
@Composable
internal fun ActivityDayParts(
    day: HistoryActivityDay,
    showsSessions: Boolean,
    formatDuration: (Long) -> String,
) {
    Column {
        if (!showsSessions) {
            day.titles.flatMap { it.parts }.forEach { ActivityPartRow(it, formatDuration) }
            return@Column
        }
        val timeFormatter = rememberTimeFormatter()
        day.sessions.forEachIndexed { index, session ->
            if (index > 0) RowGroupSeparator()
            TallyRow(
                title = "${session.startedAt.format(timeFormatter)} – ${session.endedAt.format(timeFormatter)}",
                value = formatDuration(session.durationMillis),
                heading = true,
            )
            session.parts.forEach { ActivityPartRow(it, formatDuration) }
        }
    }
}

@Composable
internal fun ActivityPartRow(
    part: HistoryActivityPart,
    formatDuration: (Long) -> String,
    underCover: Boolean = false,
) {
    TallyRow(
        title = part.title ?: stringResource(MR.strings.statistics_no_child_activity_details),
        value = formatDuration(part.durationMillis),
        mark = MaterialSymbols.Rounded.Check.takeIf { part.finished },
        markDescription = stringResource(MR.strings.statistics_finished).takeIf { part.finished },
        underCover = underCover,
    )
}
