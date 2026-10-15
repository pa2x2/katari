package eu.kanade.presentation.library.update.report

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.library.update.report.components.ActionChips
import eu.kanade.presentation.library.update.report.components.CollapsibleReportSection
import eu.kanade.presentation.library.update.report.components.FailingSourceGroup
import eu.kanade.presentation.library.update.report.components.PauseChip
import eu.kanade.presentation.library.update.report.components.PausedSourceGroup
import eu.kanade.presentation.library.update.report.components.ReportRow
import eu.kanade.presentation.library.update.report.components.ReportSectionHeader
import eu.kanade.presentation.library.update.report.components.reasonGroups
import eu.kanade.presentation.library.update.report.components.reasonSection
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportViewModel
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.entry.interactions.source.EntryWebViewResolution
import mihon.feature.library.update.report.LibraryUpdateReport
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CheckCircle
import mihon.icons.materialsymbols.rounded.Error
import mihon.icons.materialsymbols.rounded.FiberNew
import mihon.icons.materialsymbols.rounded.PauseCircle
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.rounded.SkipNext
import mihon.icons.materialsymbols.rounded.Warning
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.chip.AssistChip
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.LibraryUpdateTrigger
import tachiyomi.i18n.*
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
    state: LibraryUpdateReportViewModel.State.Ready,
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
                modifier = Modifier.padding(
                    horizontal = KatariTheme.spacing.contentInset,
                    vertical = KatariTheme.spacing.s,
                ),
                role = TextRole.Body,
                tone = ContentTone.Secondary,
            )
        }

        val (repeatedSources, failedSources) = report.failingSources.partition { it.isFailingRepeatedly }
        val failingCount = repeatedSources.sumOf { it.items.size } + report.failingRepeatedly.size
        if (failingCount > 0) {
            item(key = "header-failing") {
                ReportSectionHeader(
                    icon = MaterialSymbols.Rounded.Warning,
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
                                label = stringResource(MR.strings.action_migrate),
                                onClick = { actions.onMigrate(listOf(migration)) },
                            )
                        }
                        if (webView != null) {
                            AssistChip(
                                label = stringResource(MR.strings.library_update_report_webview),
                                onClick = { actions.onWebView(item.entry, webView) },
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
                ReportSectionHeader(
                    icon = MaterialSymbols.Rounded.Error,
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
                        IconButton(
                            icon = MaterialSymbols.Rounded.Refresh,
                            contentDescription = stringResource(MR.strings.library_update_report_retry),
                            onClick = { actions.onRetry(listOf(item.entry)) },
                        )
                    },
                )
            }
        }

        if (report.newChapters.isNotEmpty()) {
            item(key = "header-new") {
                ReportSectionHeader(
                    icon = MaterialSymbols.Rounded.FiberNew,
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
                CollapsibleReportSection(
                    icon = MaterialSymbols.Rounded.CheckCircle,
                    title = MR.strings.library_update_report_no_changes,
                    items = report.noChanges,
                    onClickEntry = actions.onClickEntry,
                )
            }
        }

        reasonSection(
            key = "skipped",
            icon = MaterialSymbols.Rounded.SkipNext,
            title = MR.strings.library_update_report_skipped,
            groups = report.skipped,
            onClickEntry = actions.onClickEntry,
        )
        if (report.skippedCount > 0) {
            item(key = "check-skipped") {
                Button(
                    label = pluralStringResource(
                        MR.plurals.library_update_report_check_skipped,
                        report.skippedCount,
                        report.skippedCount,
                    ),
                    onClick = actions.onCheckSkipped,
                    modifier = Modifier.padding(
                        horizontal = KatariTheme.spacing.contentInset,
                        vertical = KatariTheme.spacing.s,
                    ),
                    style = ButtonStyle.Tonal,
                    icon = MaterialSymbols.Rounded.Refresh,
                )
            }
        }

        val notCheckedCount = report.pausedSources.sumOf { it.items.size } + report.notChecked.values.sumOf { it.size }
        if (notCheckedCount > 0) {
            item(key = "header-not-checked") {
                ReportSectionHeader(
                    icon = MaterialSymbols.Rounded.PauseCircle,
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
                    Button(
                        label = if (paused) {
                            stringResource(MR.strings.library_update_report_resume)
                        } else {
                            stringResource(MR.strings.library_update_report_pause)
                        },
                        onClick = { actions.onSetPaused(item.entry, !paused) },
                        style = ButtonStyle.Text,
                    )
                }
            },
        )
    }
}

private fun LazyListScope.failingSourceItems(
    key: String,
    sources: List<LibraryUpdateReport.FailingSource>,
    state: LibraryUpdateReportViewModel.State.Ready,
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

private val LibraryUpdateTrigger.labelRes
    get() = when (this) {
        LibraryUpdateTrigger.AUTOMATIC -> MR.strings.library_update_trigger_automatic
        LibraryUpdateTrigger.MANUAL -> MR.strings.library_update_trigger_manual
        LibraryUpdateTrigger.SELECTION -> MR.strings.library_update_trigger_selection
    }
