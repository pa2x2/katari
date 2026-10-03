package eu.kanade.presentation.library.update.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.library.update.labelRes
import eu.kanade.presentation.library.update.report.components.ActionChips
import eu.kanade.presentation.library.update.report.components.FailingSourceGroup
import eu.kanade.presentation.library.update.report.components.PauseChip
import eu.kanade.presentation.library.update.report.components.PausedSourceGroup
import eu.kanade.presentation.library.update.report.components.ReportRow
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportScreenModel
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.entry.interactions.source.EntryWebViewResolution
import mihon.feature.library.update.report.LibraryUpdateReport
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

class LibraryUpdateReportActions(
    val onClickEntry: (Entry) -> Unit,
    val onMigrate: (List<EntryMigrationSubject>) -> Unit,
    val onWebView: (Entry, EntryWebViewResolution.Available) -> Unit,
    val onSetPaused: (Entry, Boolean) -> Unit,
    /** Pauses the source until a time in epoch milliseconds, or until resumed for null. */
    val onPauseSource: (Long, Long?) -> Unit,
    val onResumeSource: (Long) -> Unit,
    val onRetry: (List<Entry>) -> Unit,
    val onCheckSkipped: () -> Unit,
)

@Composable
fun LibraryUpdateReportContent(
    state: LibraryUpdateReportScreenModel.State.Ready,
    actions: LibraryUpdateReportActions,
    contentPadding: PaddingValues,
) {
    val report = state.report
    LazyColumn(contentPadding = contentPadding) {
        if (state.isUpdating) {
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
            item(key = "header-failing") {
                SectionHeader(
                    icon = Icons.Outlined.ReportProblem,
                    title = MR.strings.library_update_report_failing,
                    count = failingCount,
                    isError = true,
                )
            }
            failingSourceItems("failing", repeatedSources, state, actions)
            items(report.failingRepeatedly, key = { "failing-${it.entry.id}" }) { item ->
                val webView = state.webViews[item.entry.id]
                val migration = state.migrations[item.entry.id]
                Column {
                    ReportRow(
                        entry = item.entry,
                        title = item.entry.title,
                        subtitle = item.withRecheck(
                            pluralStringResource(
                                MR.plurals.library_update_report_failed_in_a_row,
                                item.status.consecutiveFailures,
                                item.status.error ?: stringResource(MR.strings.library_update_report_unknown_error),
                                item.status.consecutiveFailures,
                            ),
                        ),
                        isError = true,
                        onClick = { actions.onClickEntry(item.entry) },
                    )
                    ActionChips {
                        if (migration != null) {
                            AssistChip(
                                onClick = { actions.onMigrate(listOf(migration)) },
                                label = { Text(stringResource(MR.strings.action_migrate)) },
                            )
                        }
                        if (webView != null) {
                            AssistChip(
                                onClick = { actions.onWebView(item.entry, webView) },
                                label = { Text(stringResource(MR.strings.library_update_report_webview)) },
                            )
                        }
                        PauseChip(
                            paused = item.entry.id in state.pausedEntryIds,
                            label = stringResource(MR.strings.library_update_report_pause),
                            onPausedChange = { actions.onSetPaused(item.entry, it) },
                        )
                    }
                }
            }
        }

        val failedCount = failedSources.sumOf { it.items.size } + report.failed.size
        if (failedCount > 0) {
            item(key = "header-failed") {
                SectionHeader(
                    icon = Icons.Outlined.ErrorOutline,
                    title = MR.strings.library_update_report_failed,
                    count = failedCount,
                    isError = true,
                )
            }
            failingSourceItems("failed", failedSources, state, actions)
            items(report.failed, key = { "failed-${it.entry.id}" }) { item ->
                ReportRow(
                    entry = item.entry,
                    title = item.entry.title,
                    subtitle = item.withRecheck(
                        item.status.error ?: stringResource(MR.strings.library_update_report_unknown_error),
                    ),
                    isError = true,
                    onClick = { actions.onClickEntry(item.entry) },
                    trailing = {
                        IconButton(onClick = { actions.onRetry(listOf(item.entry)) }) {
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
            item(key = "header-new") {
                SectionHeader(
                    icon = Icons.Outlined.FiberNew,
                    title = MR.strings.library_update_report_new,
                    count = report.newChapters.size,
                )
            }
            items(report.newChapters, key = { "new-${it.entry.id}" }) { item ->
                ReportRow(
                    entry = item.entry,
                    title = item.entry.title,
                    subtitle = item.withRecheck(
                        pluralStringResource(
                            MR.plurals.library_updates_new_chapters,
                            item.status.newChapters,
                            item.status.newChapters,
                        ),
                    ),
                    onClick = { actions.onClickEntry(item.entry) },
                )
            }
        }

        if (report.noChanges.isNotEmpty()) {
            item(key = "no-changes") {
                CollapsibleSection(
                    icon = Icons.Outlined.CheckCircleOutline,
                    title = MR.strings.library_update_report_no_changes,
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

        val notCheckedCount = report.pausedSources.sumOf { it.items.size } + report.notChecked.values.sumOf { it.size }
        if (notCheckedCount > 0) {
            item(key = "header-not-checked") {
                SectionHeader(
                    icon = Icons.Outlined.PauseCircleOutline,
                    title = MR.strings.library_update_report_not_checked,
                    count = notCheckedCount,
                )
            }
        }
        items(report.pausedSources, key = { "not-checked-source-${it.sourceId}" }) { source ->
            PausedSourceGroup(
                source = source,
                name = state.sourceNames[source.sourceId],
                pause = state.sourcePauses[source.sourceId],
                actions = actions,
            )
        }
        reasonGroups(
            key = "not-checked",
            groups = report.notChecked,
            onClickEntry = actions.onClickEntry,
            // Entries set to never check were often paused from this report, so they can be resumed here too.
            itemTrailing = { item ->
                if (item.status.reason == EntryUpdateDecisionReason.ENTRY_NEVER) {
                    val paused = item.entry.id in state.pausedEntryIds
                    TextButton(onClick = { actions.onSetPaused(item.entry, !paused) }) {
                        Text(
                            if (paused) {
                                stringResource(MR.strings.library_update_report_resume)
                            } else {
                                stringResource(MR.strings.library_update_report_pause)
                            },
                        )
                    }
                }
            },
        )
    }
}

private fun LazyListScope.failingSourceItems(
    key: String,
    sources: List<LibraryUpdateReport.FailingSource>,
    state: LibraryUpdateReportScreenModel.State.Ready,
    actions: LibraryUpdateReportActions,
) {
    items(sources, key = { "$key-source-${it.sourceId}-${it.error}" }) { source ->
        FailingSourceGroup(
            source = source,
            name = state.sourceNames[source.sourceId],
            pause = state.sourcePauses[source.sourceId],
            migrations = source.items.mapNotNull { state.migrations[it.entry.id] },
            actions = actions,
        )
    }
}

@Composable
private fun LibraryUpdateReport.Item.withRecheck(subtitle: String): String {
    return if (isRechecked) stringResource(MR.strings.library_update_report_with_rechecked, subtitle) else subtitle
}

@Composable
private fun FoldedItems(
    items: List<LibraryUpdateReport.Item>,
    onClickEntry: (Entry) -> Unit,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)? = null,
) {
    items.forEach { item ->
        ReportRow(
            entry = item.entry,
            title = item.entry.title,
            subtitle = if (item.isRechecked) stringResource(MR.strings.library_update_report_rechecked) else null,
            onClick = { onClickEntry(item.entry) },
            trailing = itemTrailing?.let { { it(item) } },
        )
    }
}

private val LibraryUpdateTrigger.labelRes
    get() = when (this) {
        LibraryUpdateTrigger.AUTOMATIC -> MR.strings.library_update_trigger_automatic
        LibraryUpdateTrigger.MANUAL -> MR.strings.library_update_trigger_manual
        LibraryUpdateTrigger.SELECTION -> MR.strings.library_update_trigger_selection
    }

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: StringResource,
    count: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.padding(
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
        trailing?.invoke()
    }
}

/** A section whose entries need no action, so they stay folded under its header until asked for. */
@Composable
private fun CollapsibleSection(
    icon: ImageVector,
    title: StringResource,
    items: List<LibraryUpdateReport.Item>,
    onClickEntry: (Entry) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        SectionHeader(
            icon = icon,
            title = title,
            count = items.size,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            trailing = {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                )
            },
        )
        if (expanded) {
            FoldedItems(items = items, onClickEntry = onClickEntry)
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
    item(key = "header-$key") {
        SectionHeader(icon = icon, title = title, count = groups.values.sumOf { it.size })
    }
    reasonGroups(key = key, groups = groups, onClickEntry = onClickEntry)
}

/** @param itemTrailing shown at the end of each entry's row once a group is unfolded. */
private fun LazyListScope.reasonGroups(
    key: String,
    groups: Map<EntryUpdateDecisionReason, List<LibraryUpdateReport.Item>>,
    onClickEntry: (Entry) -> Unit,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)? = null,
) {
    groups.forEach { (reason, items) ->
        item(key = "$key-${reason.name}") {
            CollapsibleGroup(
                title = stringResource(reason.labelRes),
                items = items,
                onClickEntry = onClickEntry,
                itemTrailing = itemTrailing,
            )
        }
    }
}

@Composable
private fun CollapsibleGroup(
    title: String,
    items: List<LibraryUpdateReport.Item>,
    onClickEntry: (Entry) -> Unit,
    itemTrailing: (@Composable (LibraryUpdateReport.Item) -> Unit)?,
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
            FoldedItems(items = items, onClickEntry = onClickEntry, itemTrailing = itemTrailing)
        }
    }
}
