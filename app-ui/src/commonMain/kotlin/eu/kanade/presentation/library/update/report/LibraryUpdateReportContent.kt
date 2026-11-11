package eu.kanade.presentation.library.update.report

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.migration.EntryMigrationSubject
import app.katari.entry.interactions.source.EntryWebViewResolution
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CheckCircle
import app.katari.icons.materialsymbols.rounded.Error
import app.katari.icons.materialsymbols.rounded.FiberNew
import app.katari.icons.materialsymbols.rounded.PauseCircle
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.icons.materialsymbols.rounded.SkipNext
import app.katari.icons.materialsymbols.rounded.Warning
import app.katari.library.update.report.LibraryUpdateReport
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.chip.AssistChip
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.presentation.library.update.report.components.ActionChips
import eu.kanade.presentation.library.update.report.components.FailingSourceGroup
import eu.kanade.presentation.library.update.report.components.NewChaptersRow
import eu.kanade.presentation.library.update.report.components.PauseChip
import eu.kanade.presentation.library.update.report.components.PausedSourceGroup
import eu.kanade.presentation.library.update.report.components.ReportProgressCard
import eu.kanade.presentation.library.update.report.components.ReportRow
import eu.kanade.presentation.library.update.report.components.ReportRunNote
import eu.kanade.presentation.library.update.report.components.ReportSectionHeader
import eu.kanade.presentation.library.update.report.components.SharedFailureGroup
import eu.kanade.presentation.library.update.report.components.foldedSection
import eu.kanade.presentation.library.update.report.components.reasonGroups
import eu.kanade.presentation.library.update.report.components.reasonSection
import eu.kanade.presentation.library.update.report.components.withRecheck
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportViewModel
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

class LibraryUpdateReportActions(
    val onClickEntry: (Entry) -> Unit,
    /** Null where entries can't be migrated, so the report doesn't offer it. */
    val onMigrate: ((List<EntryMigrationSubject>) -> Unit)?,
    /** Null where the app has no web view, so the report doesn't offer it. */
    val onWebView: ((Entry, EntryWebViewResolution.Available) -> Unit)?,
    val onSetPaused: (Entry, Boolean) -> Unit,
    /** Pauses the source until a time in epoch milliseconds, or until resumed for null. */
    val onPauseSource: (Long, Long?) -> Unit,
    val onResumeSource: (Long) -> Unit,
    val onRetry: (List<Entry>) -> Unit,
    val onCheckSkipped: () -> Unit,
    val onStopUpdate: () -> Unit,
    val onOpenChapter: (Entry, EntryChapter) -> Unit,
)

@Composable
fun LibraryUpdateReportContent(
    state: LibraryUpdateReportViewModel.State.Ready,
    actions: LibraryUpdateReportActions,
    contentPadding: PaddingValues,
) {
    val report = state.report
    val sourceNameOf = { entry: Entry -> state.sourceNames[entry.source] }
    RowList(contentPadding = contentPadding) {
        state.update?.let { update ->
            item(key = "running") {
                ReportProgressCard(update = update, onStop = actions.onStopUpdate)
            }
        }
        item(key = "header") {
            ReportRunNote(report.run, report.checked)
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
            val cardSize = repeatedSources.size + report.failingRepeatedly.size
            failingSourceItems("failing", repeatedSources, cardSize, state, actions)
            itemsIndexed(report.failingRepeatedly, key = { _, it -> "failing-${it.entry.id}" }) { index, item ->
                val webView = state.webViews[item.entry.id]
                val migration = state.migrations[item.entry.id]
                RowGroupItem(position = RowGroupPosition.of(repeatedSources.size + index, cardSize)) {
                    ReportRow(
                        entry = item.entry,
                        title = item.entry.title,
                        sourceName = sourceNameOf(item.entry),
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
                        actions = {
                            ActionChips {
                                val onMigrate = actions.onMigrate
                                if (migration != null && onMigrate != null) {
                                    AssistChip(
                                        label = stringResource(MR.strings.action_migrate),
                                        onClick = { onMigrate(listOf(migration)) },
                                    )
                                }
                                val onWebView = actions.onWebView
                                if (webView != null && onWebView != null) {
                                    AssistChip(
                                        label = stringResource(MR.strings.library_update_report_webview),
                                        onClick = { onWebView(item.entry, webView) },
                                    )
                                }
                                PauseChip(
                                    paused = item.entry.id in state.pausedEntryIds,
                                    label = stringResource(MR.strings.library_update_report_pause),
                                    onPausedChange = { actions.onSetPaused(item.entry, it) },
                                )
                            }
                        },
                    )
                }
            }
        }

        val sharedFailure = report.sharedFailure
        val failedEntries = sharedFailure?.items.orEmpty().map { it.entry } +
            failedSources.flatMap { source -> source.items.map { it.entry } } +
            report.failed.map { it.entry }
        if (failedEntries.isNotEmpty()) {
            item(key = "header-failed") {
                ReportSectionHeader(
                    icon = MaterialSymbols.Rounded.Error,
                    title = MR.strings.library_update_report_failed,
                    count = failedEntries.size,
                    isError = true,
                    trailing = if (failedEntries.size > 1) {
                        {
                            Button(
                                label = stringResource(MR.strings.library_update_report_retry_all),
                                onClick = { actions.onRetry(failedEntries) },
                                style = ButtonStyle.Text,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            val sharedRows = if (sharedFailure != null) 1 else 0
            val cardSize = sharedRows + failedSources.size + report.failed.size
            if (sharedFailure != null) {
                item(key = "failed-shared") {
                    RowGroupItem(position = RowGroupPosition.of(0, cardSize)) {
                        SharedFailureGroup(failure = sharedFailure, sourceNameOf = sourceNameOf, actions = actions)
                    }
                }
            }
            failingSourceItems("failed", failedSources, cardSize, state, actions, offset = sharedRows)
            itemsIndexed(report.failed, key = { _, it -> "failed-${it.entry.id}" }) { index, item ->
                RowGroupItem(position = RowGroupPosition.of(sharedRows + failedSources.size + index, cardSize)) {
                    ReportRow(
                        entry = item.entry,
                        title = item.entry.title,
                        sourceName = sourceNameOf(item.entry),
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
        }

        if (report.newChapters.isNotEmpty()) {
            item(key = "header-new") {
                ReportSectionHeader(
                    icon = MaterialSymbols.Rounded.FiberNew,
                    title = MR.strings.library_update_report_new,
                    count = report.newChapters.size,
                )
            }
            rowGroupItems(report.newChapters, key = { "new-${it.entry.id}" }) { item ->
                NewChaptersRow(
                    item = item,
                    chapters = state.newChapters[item.entry.id].orEmpty(),
                    sourceName = sourceNameOf(item.entry),
                    onClickEntry = actions.onClickEntry,
                    onOpenChapter = actions.onOpenChapter,
                )
            }
        }

        foldedSection(
            key = "no-changes",
            icon = MaterialSymbols.Rounded.CheckCircle,
            title = MR.strings.library_update_report_no_changes,
            items = report.noChanges,
            sourceNameOf = sourceNameOf,
            onClickEntry = actions.onClickEntry,
        )

        reasonSection(
            key = "skipped",
            icon = MaterialSymbols.Rounded.SkipNext,
            title = MR.strings.library_update_report_skipped,
            groups = report.skipped,
            sourceNameOf = sourceNameOf,
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
                    modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
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
        val notCheckedCardSize = report.pausedSources.size + report.notChecked.size
        itemsIndexed(report.pausedSources, key = { _, it -> "not-checked-source-${it.sourceId}" }) { index, source ->
            RowGroupItem(position = RowGroupPosition.of(index, notCheckedCardSize)) {
                PausedSourceGroup(
                    source = source,
                    name = state.sourceNames[source.sourceId],
                    pause = state.sourcePauses[source.sourceId],
                    actions = actions,
                )
            }
        }
        reasonGroups(
            key = "not-checked",
            groups = report.notChecked,
            sourceNameOf = sourceNameOf,
            onClickEntry = actions.onClickEntry,
            offset = report.pausedSources.size,
            cardSize = notCheckedCardSize,
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

/** Sources whose entries failed alike, as rows of their section's card of [cardSize] rows from the [offset]th on. */
private fun LazyListScope.failingSourceItems(
    key: String,
    sources: List<LibraryUpdateReport.FailingSource>,
    cardSize: Int,
    state: LibraryUpdateReportViewModel.State.Ready,
    actions: LibraryUpdateReportActions,
    offset: Int = 0,
) {
    itemsIndexed(sources, key = { _, it -> "$key-source-${it.sourceId}-${it.error}" }) { index, source ->
        RowGroupItem(position = RowGroupPosition.of(offset + index, cardSize)) {
            FailingSourceGroup(
                source = source,
                name = state.sourceNames[source.sourceId],
                pause = state.sourcePauses[source.sourceId],
                migrations = source.items.mapNotNull { state.migrations[it.entry.id] },
                actions = actions,
            )
        }
    }
}
