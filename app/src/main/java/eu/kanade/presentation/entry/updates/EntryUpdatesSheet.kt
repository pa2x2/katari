package eu.kanade.presentation.entry.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.library.update.labelRes
import eu.kanade.presentation.library.update.sourcePauseEndText
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import eu.kanade.tachiyomi.ui.entry.updates.EntryUpdatesViewModel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.PauseCircle
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.ChoiceRow
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * How library updates treat one entry: its mode, why the latest update did what it did, which of its sources are paused,
 * and its release estimate.
 * Refreshing from the entry's own screen always checks it, so none of this affects that.
 */
@Composable
fun EntryUpdatesSheet(
    entry: Entry,
    mode: EntryUpdateMode,
    status: EntryUpdateStatus?,
    categories: Map<Long, Category>,
    pausedSources: List<EntryUpdatesViewModel.PausedSource>,
    releaseEstimate: String,
    onModeSelected: (EntryUpdateMode) -> Unit,
    onResumeSource: (Long) -> Unit,
    onReleaseEstimateClicked: (() -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    Sheet(onDismissRequest = onDismissRequest) {
        Column {
            SheetHeader(title = stringResource(MR.strings.entry_updates_title))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = KatariTheme.spacing.s),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            ) {
                if (mode == EntryUpdateMode.FOLLOW_RULES) {
                    DecisionCard(text = decisionText(status, categories))
                    // Always check overrides a paused source, so the pause only matters while the entry follows the rules.
                    pausedSources.forEach { source ->
                        PausedSourceCard(source = source, onResume = { onResumeSource(source.id) })
                    }
                }
                if (entry.updateStrategy == EntryUpdateStrategy.ONLY_FETCH_ONCE) {
                    DecisionCard(text = stringResource(MR.strings.entry_updates_fetch_once))
                }
                EntryUpdateMode.entries.forEach { option ->
                    ChoiceRow(
                        title = stringResource(option.titleRes),
                        selected = option == mode,
                        onClick = { onModeSelected(option) },
                        subtitle = stringResource(option.summaryRes),
                    )
                }
                if (onReleaseEstimateClicked != null) {
                    SettingsRow(
                        title = stringResource(MR.strings.entry_updates_release_estimate),
                        subtitle = releaseEstimate,
                        onClick = onReleaseEstimateClicked,
                    )
                } else {
                    Text(
                        text = releaseEstimate,
                        role = TextRole.Supporting,
                        modifier = Modifier.padding(
                            horizontal = KatariTheme.spacing.rowInset,
                            vertical = KatariTheme.spacing.s,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdatesNoticeCard(content: @Composable RowScope.() -> Unit) {
    Surface(
        container = SurfaceContainer.Highest,
        shape = KatariTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.xs),
    ) {
        Row(
            modifier = Modifier.padding(start = KatariTheme.spacing.m),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun DecisionCard(text: String) {
    UpdatesNoticeCard {
        Icon(
            imageVector = MaterialSymbols.Rounded.Info,
            contentDescription = null,
            tone = ContentTone.Primary,
        )
        Text(
            text = text,
            role = TextRole.Body,
            modifier = Modifier.padding(vertical = KatariTheme.spacing.m).padding(end = KatariTheme.spacing.m),
        )
    }
}

@Composable
private fun PausedSourceCard(source: EntryUpdatesViewModel.PausedSource, onResume: () -> Unit) {
    UpdatesNoticeCard {
        Icon(
            imageVector = MaterialSymbols.Rounded.PauseCircle,
            contentDescription = null,
            tone = ContentTone.Primary,
        )
        Text(
            text = if (source.until == null) {
                stringResource(MR.strings.entry_updates_source_paused, source.name)
            } else {
                stringResource(
                    MR.strings.entry_updates_source_paused_until,
                    source.name,
                    sourcePauseEndText(source.until),
                )
            },
            role = TextRole.Body,
            modifier = Modifier.weight(1f),
        )
        Button(
            label = stringResource(MR.strings.library_update_report_resume),
            onClick = onResume,
            modifier = Modifier.padding(KatariTheme.spacing.xs),
            style = ButtonStyle.Text,
        )
    }
}

private val EntryUpdateMode.titleRes
    get() = when (this) {
        EntryUpdateMode.FOLLOW_RULES -> MR.strings.entry_updates_mode_follow
        EntryUpdateMode.ALWAYS -> MR.strings.entry_updates_mode_always
        EntryUpdateMode.NEVER -> MR.strings.entry_updates_mode_never
    }

private val EntryUpdateMode.summaryRes
    get() = when (this) {
        EntryUpdateMode.FOLLOW_RULES -> MR.strings.entry_updates_mode_follow_summary
        EntryUpdateMode.ALWAYS -> MR.strings.entry_updates_mode_always_summary
        EntryUpdateMode.NEVER -> MR.strings.entry_updates_mode_never_summary
    }

/** Names the step that decided the latest update, including the category whose switch or rules it was. */
@Composable
private fun decisionText(status: EntryUpdateStatus?, categories: Map<Long, Category>): String {
    if (status == null) return stringResource(MR.strings.entry_updates_why_none)
    val category = status.reasonCategoryId?.let(categories::get)?.visualName
    val reason = status.reason
    return when (status.outcome) {
        EntryUpdateOutcome.SKIPPED -> if (reason != null && category != null) {
            stringResource(MR.strings.entry_updates_why_skipped_category, stringResource(reason.labelRes), category)
        } else {
            stringResource(MR.strings.entry_updates_why_skipped, reason?.let { stringResource(it.labelRes) }.orEmpty())
        }
        EntryUpdateOutcome.NOT_CHECKED -> if (reason == EntryUpdateDecisionReason.CATEGORY_OFF && category != null) {
            stringResource(MR.strings.entry_updates_why_category_off, category)
        } else {
            stringResource(
                MR.strings.entry_updates_why_not_checked,
                reason?.let { stringResource(it.labelRes) }.orEmpty(),
            )
        }
        EntryUpdateOutcome.FAILED -> stringResource(
            MR.strings.entry_updates_why_failed,
            pluralStringResource(
                MR.plurals.library_update_report_failed_in_a_row,
                status.consecutiveFailures,
                status.error ?: stringResource(MR.strings.library_update_report_unknown_error),
                status.consecutiveFailures,
            ),
        )
        EntryUpdateOutcome.NEW_CHAPTERS, EntryUpdateOutcome.NO_CHANGES -> stringResource(
            MR.strings.entry_updates_why_checked,
            relativeTimeSpanString(status.lastCheckedAt ?: status.decidedAt),
        )
    }
}
