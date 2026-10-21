package eu.kanade.presentation.history.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.more.stats.components.StatisticsDurationPrecision
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.GroupCard
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.history.model.activity.HistoryActivitySegmentDetail
import tachiyomi.domain.history.model.activity.HistoryActivitySessionDetail
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * @param showEntry false when the list is already scoped to the session's title: the card then leads with what was read
 * instead of repeating the title, and doesn't link back to it.
 */
@Composable
internal fun ActivitySessionCard(
    session: HistoryActivitySessionDetail,
    showType: Boolean,
    showEntry: Boolean,
    onClick: (Long) -> Unit,
) {
    val formatDuration = rememberStatisticsDurationFormatter(StatisticsDurationPrecision.SECONDS)
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
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

    GroupCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (showEntry) Modifier.clickable { onClick(session.entryId) } else Modifier)
                    .padding(KatariTheme.spacing.m),
                verticalAlignment = Alignment.Top,
            ) {
                if (showEntry) {
                    EntryCover.Book(
                        data = session.coverData,
                        contentDescription = "",
                        modifier = Modifier.height(BrowseMetrics.listCoverHeight),
                        frame = CoverFrame.Thumbnail,
                    )
                    Spacer(Modifier.width(KatariTheme.spacing.m))
                }
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        val isUndetailedHeadline = !showEntry && childDurations.isEmpty()
                        Text(
                            text = when {
                                showEntry -> session.entryTitle
                                isUndetailedHeadline -> stringResource(MR.strings.statistics_no_child_activity_details)
                                else -> listOf(childDurations.first().first, childDurations.last().first)
                                    .distinct()
                                    .joinToString(" – ")
                            },
                            modifier = Modifier.weight(1f),
                            role = if (isUndetailedHeadline) TextRole.Body else TextRole.SectionTitle,
                            tone = if (isUndetailedHeadline) ContentTone.Secondary else ContentTone.Inherit,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(KatariTheme.spacing.s))
                        Pill(
                            text = formatDuration(session.durationMillis),
                            container = SurfaceContainer.PrimaryContainer,
                        )
                    }
                    when {
                        // Without the title, the headline already names a lone chapter or a missing breakdown.
                        !showEntry && childDurations.size <= 1 -> Unit
                        childDurations.isEmpty() -> {
                            Spacer(Modifier.height(KatariTheme.spacing.s))
                            Text(
                                text = stringResource(MR.strings.statistics_no_child_activity_details),
                                role = TextRole.Supporting,
                            )
                        }
                        !hasLongChildList -> {
                            childDurations.forEach { (title, duration) ->
                                Spacer(Modifier.height(KatariTheme.spacing.s))
                                ActivityChildRow(
                                    title = title,
                                    duration = duration.takeIf { childDurations.size > 1 }?.let(formatDuration),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(KatariTheme.spacing.s))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showType) {
                            session.entryType.InlineEntryTypeIndicator()
                            Spacer(Modifier.width(KatariTheme.spacing.xs))
                            Text(
                                text = stringResource(typePresentation.displayNameLabel) + " · ",
                                role = TextRole.Supporting,
                            )
                        }
                        Text(
                            text = "$startedAt – $endedAt",
                            role = TextRole.Supporting,
                        )
                    }
                }
            }
            if (hasLongChildList) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = KatariTheme.spacing.m))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { childrenExpanded = !childrenExpanded }
                        .heightIn(min = KatariTheme.sizes.touchTarget)
                        .padding(horizontal = KatariTheme.spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = pluralStringResource(
                            typePresentation.childCountPlural,
                            childDurations.size,
                            childDurations.size,
                        ),
                        modifier = Modifier.weight(1f),
                        role = TextRole.Label,
                    )
                    Text(
                        text = formatDuration(childDurations.sumOf { it.second }),
                        role = TextRole.Supporting,
                    )
                    Spacer(Modifier.width(KatariTheme.spacing.s))
                    Icon(
                        imageVector = if (childrenExpanded) {
                            MaterialSymbols.Rounded.KeyboardArrowUp
                        } else {
                            MaterialSymbols.Rounded.KeyboardArrowDown
                        },
                        contentDescription = stringResource(
                            if (childrenExpanded) MR.strings.action_collapse else MR.strings.action_expand,
                        ),
                    )
                }
                if (childrenExpanded) {
                    Column(
                        Modifier.padding(
                            start = KatariTheme.spacing.m,
                            end = KatariTheme.spacing.m,
                            bottom = KatariTheme.spacing.s,
                        ),
                    ) {
                        childDurations.forEachIndexed { index, (title, duration) ->
                            if (index > 0) {
                                HorizontalDivider()
                            }
                            ActivityChildRow(
                                title = title,
                                duration = formatDuration(duration),
                                modifier = Modifier.padding(vertical = KatariTheme.spacing.s),
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
            role = TextRole.Supporting,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        duration?.let {
            Spacer(Modifier.width(KatariTheme.spacing.m))
            Text(
                text = it,
                role = TextRole.Badge,
                tone = ContentTone.Secondary,
            )
        }
    }
}

private const val MAX_VISIBLE_CHILDREN = 3
