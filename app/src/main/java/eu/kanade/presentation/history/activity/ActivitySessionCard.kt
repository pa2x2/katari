package eu.kanade.presentation.history.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentDetail
import tachiyomi.domain.history.model.activity.HistoryActivitySessionDetail
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun ActivitySessionCard(
    session: HistoryActivitySessionDetail,
    showType: Boolean,
    onClick: (Long) -> Unit,
) {
    val formatDuration = rememberActivityDurationFormatter()
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val zone = remember(session.segments) {
        session.segments.firstOrNull()?.timeZoneId
            ?.let { runCatching { ZoneId.of(it) }.getOrNull() }
            ?: ZoneId.systemDefault()
    }
    val startedAt = Instant.ofEpochMilli(session.startedAtEpochMillis).atZone(zone).format(timeFormatter)
    val endedAt = Instant.ofEpochMilli(session.endedAtEpochMillis).atZone(zone).format(timeFormatter)
    val childDurations = remember(session.segments) {
        session.segments
            .filter { !it.chapterTitle.isNullOrBlank() }
            .groupBy(HistoryActivitySegmentDetail::chapterId)
            .map { (_, segments) -> segments.first().chapterTitle.orEmpty() to segments.sumOf { it.durationMillis } }
    }
    val typePresentation = session.entryType.entryTypePresentation()
    val hasLongChildList = childDurations.size > MAX_VISIBLE_CHILDREN
    var childrenExpanded by rememberSaveable(session.sessionId) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(session.entryId) }
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                EntryCover.Book(
                    data = session.coverData,
                    contentDescription = "",
                    modifier = Modifier.width(64.dp),
                    shape = MaterialTheme.shapes.small,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text(
                            text = session.entryTitle,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = formatDuration(session.durationMillis),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                    when {
                        childDurations.isEmpty() -> {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(MR.strings.statistics_no_child_activity_details),
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        !hasLongChildList -> {
                            childDurations.forEach { (title, duration) ->
                                Spacer(Modifier.height(8.dp))
                                ActivityChildRow(
                                    title = title,
                                    duration = duration.takeIf { childDurations.size > 1 }?.let(formatDuration),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showType) {
                            session.entryType.InlineEntryTypeIndicator()
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(typePresentation.displayNameLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = " · ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = "$startedAt – $endedAt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (session.completionCount > 0L) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = pluralStringResource(
                                        typePresentation.completedChildCountPlural,
                                        session.completionCount.toInt(),
                                        session.completionCount,
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
            if (hasLongChildList) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { childrenExpanded = !childrenExpanded }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pluralStringResource(
                            typePresentation.childCountPlural,
                            childDurations.size,
                            childDurations.size,
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = formatDuration(childDurations.sumOf { it.second }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = if (childrenExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = stringResource(
                            if (childrenExpanded) MR.strings.action_collapse else MR.strings.action_expand,
                        ),
                    )
                }
                if (childrenExpanded) {
                    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                        childDurations.forEachIndexed { index, (title, duration) ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            ActivityChildRow(
                                title = title,
                                duration = formatDuration(duration),
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityChildRow(
    title: String,
    duration: String?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        duration?.let {
            Spacer(Modifier.width(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val MAX_VISIBLE_CHILDREN = 3

@Composable
private fun rememberActivityDurationFormatter(): (Long) -> String {
    val locale = Locale.getDefault()
    return remember(locale) {
        val numbers = NumberFormat.getIntegerInstance(locale)
        val formatter: (Long) -> String = { durationMillis ->
            val totalSeconds = durationMillis.coerceAtLeast(0L) / 1_000L
            val totalMinutes = totalSeconds / 60L
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            val seconds = totalSeconds % 60L
            when {
                hours > 0L && minutes > 0L -> "${numbers.format(hours)}h ${numbers.format(minutes)}m"
                hours > 0L -> "${numbers.format(hours)}h"
                minutes > 0L && seconds > 0L -> "${numbers.format(minutes)}m ${numbers.format(seconds)}s"
                minutes > 0L -> "${numbers.format(minutes)}m"
                else -> "${numbers.format(seconds)}s"
            }
        }
        formatter
    }
}
