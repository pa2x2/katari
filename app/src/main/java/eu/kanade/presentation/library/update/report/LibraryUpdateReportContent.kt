package eu.kanade.presentation.library.update.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FiberNew
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.presentation.library.update.labelRes
import eu.kanade.presentation.util.relativeTimeSpanString
import mihon.feature.library.update.report.LibraryUpdateReport
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

class LibraryUpdateReportActions(
    val onClickEntry: (Entry) -> Unit,
    val onMigrate: (Entry) -> Unit,
    val onWebView: (Entry) -> Unit,
    val onSetPaused: (Entry, Boolean) -> Unit,
    val onSetSourcePaused: (Long, Boolean) -> Unit,
    val onRetry: (Entry) -> Unit,
    val onCheckSkipped: () -> Unit,
)

@Composable
fun LibraryUpdateReportContent(
    report: LibraryUpdateReport,
    sourceNames: Map<Long, String>,
    pausedSourceIds: Set<Long>,
    pausedEntryIds: Set<Long>,
    actions: LibraryUpdateReportActions,
    contentPadding: PaddingValues,
) {
    LazyColumn(contentPadding = contentPadding) {
        if (report.run.finishedAt == null) {
            item(key = "running") {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        item(key = "header") {
            Text(
                text = listOf(
                    relativeTimeSpanString(report.run.startedAt),
                    stringResource(report.run.trigger.labelRes),
                    pluralStringResource(
                        MR.plurals.library_update_report_considered,
                        report.run.librarySize,
                        report.run.librarySize,
                    ),
                )
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium, vertical = 8.dp),
            )
        }

        val (repeatedSources, failedSources) = report.failingSources.partition { it.isFailingRepeatedly }
        val failingCount = repeatedSources.sumOf { it.items.size } + report.failingRepeatedly.size
        if (failingCount > 0) {
            sectionHeader(
                key = "failing",
                icon = Icons.Outlined.ReportProblem,
                title = MR.strings.library_update_report_failing,
                count = failingCount,
                isError = true,
            )
            sourceFailureItems("failing", repeatedSources, sourceNames, pausedSourceIds, actions)
            items(report.failingRepeatedly, key = { "failing-${it.entry.id}" }) { item ->
                Column {
                    ReportRow(
                        entry = item.entry,
                        title = item.entry.title,
                        subtitle = pluralStringResource(
                            MR.plurals.library_update_report_failed_in_a_row,
                            item.status.consecutiveFailures,
                            item.status.error ?: stringResource(MR.strings.library_update_report_unknown_error),
                            item.status.consecutiveFailures,
                        ),
                        isError = true,
                        onClick = { actions.onClickEntry(item.entry) },
                    )
                    ActionChips {
                        AssistChip(
                            onClick = { actions.onMigrate(item.entry) },
                            label = { Text(stringResource(MR.strings.action_migrate)) },
                        )
                        AssistChip(
                            onClick = { actions.onWebView(item.entry) },
                            label = { Text(stringResource(MR.strings.library_update_report_webview)) },
                        )
                        PauseChip(
                            paused = item.entry.id in pausedEntryIds,
                            label = MR.strings.library_update_report_pause,
                            onPausedChange = { actions.onSetPaused(item.entry, it) },
                        )
                    }
                }
            }
        }

        val failedCount = failedSources.sumOf { it.items.size } + report.failed.size
        if (failedCount > 0) {
            sectionHeader(
                key = "failed",
                icon = Icons.Outlined.ErrorOutline,
                title = MR.strings.library_update_report_failed,
                count = failedCount,
                isError = true,
            )
            sourceFailureItems("failed", failedSources, sourceNames, pausedSourceIds, actions)
            items(report.failed, key = { "failed-${it.entry.id}" }) { item ->
                ReportRow(
                    entry = item.entry,
                    title = item.entry.title,
                    subtitle = item.status.error ?: stringResource(MR.strings.library_update_report_unknown_error),
                    isError = true,
                    onClick = { actions.onClickEntry(item.entry) },
                    trailing = {
                        IconButton(onClick = { actions.onRetry(item.entry) }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(MR.strings.library_update_report_retry),
                            )
                        }
                    },
                )
            }
        }

        if (report.newChapters.isNotEmpty()) {
            sectionHeader(
                key = "new",
                icon = Icons.Outlined.FiberNew,
                title = MR.strings.library_update_report_new,
                count = report.newChapters.size,
            )
            items(report.newChapters, key = { "new-${it.entry.id}" }) { item ->
                ReportRow(
                    entry = item.entry,
                    title = item.entry.title,
                    subtitle = pluralStringResource(
                        MR.plurals.library_updates_new_chapters,
                        item.status.newChapters,
                        item.status.newChapters,
                    ),
                    onClick = { actions.onClickEntry(item.entry) },
                )
            }
        }

        if (report.noChanges.isNotEmpty()) {
            item(key = "no-changes") {
                CollapsibleGroup(
                    title = stringResource(MR.strings.library_update_report_no_changes),
                    items = report.noChanges,
                    onClickEntry = actions.onClickEntry,
                )
            }
        }

        reasonSection(
            key = "skipped",
            icon = Icons.Outlined.SkipNext,
            title = MR.strings.library_update_report_skipped,
            groups = report.skipped,
            onClickEntry = actions.onClickEntry,
        )
        if (report.skippedCount > 0) {
            item(key = "check-skipped") {
                FilledTonalButton(
                    onClick = actions.onCheckSkipped,
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        pluralStringResource(
                            MR.plurals.library_update_report_check_skipped,
                            report.skippedCount,
                            report.skippedCount,
                        ),
                    )
                }
            }
        }

        reasonSection(
            key = "not-checked",
            icon = Icons.Outlined.PauseCircleOutline,
            title = MR.strings.library_update_report_not_checked,
            groups = report.notChecked,
            onClickEntry = actions.onClickEntry,
        )
    }
}

/** One row per source whose checks mostly failed the same way, since pausing the source is the fix. */
private fun LazyListScope.sourceFailureItems(
    key: String,
    sources: List<LibraryUpdateReport.FailingSource>,
    sourceNames: Map<Long, String>,
    pausedSourceIds: Set<Long>,
    actions: LibraryUpdateReportActions,
) {
    items(sources, key = { "$key-source-${it.sourceId}-${it.error}" }) { source ->
        Column {
            ReportRow(
                entry = null,
                title = pluralStringResource(
                    MR.plurals.library_update_report_source_entries,
                    source.items.size,
                    sourceNames[source.sourceId].orEmpty(),
                    source.items.size,
                ),
                subtitle = source.error ?: stringResource(MR.strings.library_update_report_unknown_error),
                isError = true,
            )
            ActionChips(start = SOURCE_ACTIONS_START) {
                PauseChip(
                    paused = source.sourceId in pausedSourceIds,
                    label = MR.strings.library_update_report_pause_source,
                    onPausedChange = { actions.onSetSourcePaused(source.sourceId, it) },
                )
            }
        }
    }
}

private val LibraryUpdateTrigger.labelRes
    get() = when (this) {
        LibraryUpdateTrigger.AUTOMATIC -> MR.strings.library_update_trigger_automatic
        LibraryUpdateTrigger.MANUAL -> MR.strings.library_update_trigger_manual
        LibraryUpdateTrigger.SELECTION -> MR.strings.library_update_trigger_selection
    }

private fun LazyListScope.sectionHeader(
    key: String,
    icon: ImageVector,
    title: StringResource,
    count: Int,
    isError: Boolean = false,
) {
    item(key = "header-$key") {
        val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Row(
            modifier = Modifier.padding(
                start = MaterialTheme.padding.medium,
                end = MaterialTheme.padding.medium,
                top = 16.dp,
                bottom = 4.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                color = color,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LazyListScope.reasonSection(
    key: String,
    icon: ImageVector,
    title: StringResource,
    groups: Map<EntryUpdateDecisionReason, List<LibraryUpdateReport.Item>>,
    onClickEntry: (Entry) -> Unit,
) {
    if (groups.isEmpty()) return
    sectionHeader(key = key, icon = icon, title = title, count = groups.values.sumOf { it.size })
    groups.forEach { (reason, items) ->
        item(key = "$key-${reason.name}") {
            CollapsibleGroup(
                title = stringResource(reason.labelRes),
                items = items,
                onClickEntry = onClickEntry,
            )
        }
    }
}

@Composable
private fun CollapsibleGroup(
    title: String,
    items: List<LibraryUpdateReport.Item>,
    onClickEntry: (Entry) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = MaterialTheme.padding.medium, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = pluralStringResource(MR.plurals.library_updates_entries, items.size, items.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
            )
        }
        if (expanded) {
            items.forEach { item ->
                ReportRow(
                    entry = item.entry,
                    title = item.entry.title,
                    subtitle = null,
                    onClick = { onClickEntry(item.entry) },
                )
            }
        }
    }
}

@Composable
private fun ReportRow(
    entry: Entry?,
    title: String,
    subtitle: String?,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(56.dp)
            .padding(horizontal = MaterialTheme.padding.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry != null) {
            EntryCover.Square(
                data = entry.asEntryCover(),
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .fillMaxHeight(),
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

// Lines the chips up with the row's title: past the cover for entries, past the padding for sources.
private val ENTRY_ACTIONS_START = 76.dp
private val SOURCE_ACTIONS_START = 32.dp

@Composable
private fun PauseChip(paused: Boolean, label: StringResource, onPausedChange: (Boolean) -> Unit) {
    FilterChip(
        selected = paused,
        onClick = { onPausedChange(!paused) },
        label = { Text(stringResource(label)) },
        leadingIcon = if (paused) {
            { Icon(imageVector = Icons.Outlined.Check, contentDescription = null) }
        } else {
            null
        },
    )
}

@Composable
private fun ActionChips(start: Dp = ENTRY_ACTIONS_START, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.padding(start = start, end = MaterialTheme.padding.medium, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}
