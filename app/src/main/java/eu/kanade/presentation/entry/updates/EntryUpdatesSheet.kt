package eu.kanade.presentation.entry.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.library.update.labelRes
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.update.model.EntryUpdateDecisionReason
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.NavigationItem
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * How library updates treat one entry: its mode, why the latest update did what it did, and its release estimate.
 * Refreshing from the entry's own screen always checks it, so none of this affects that.
 */
@Composable
fun EntryUpdatesSheet(
    entry: Entry,
    mode: EntryUpdateMode,
    status: EntryUpdateStatus?,
    categories: Map<Long, Category>,
    releaseEstimate: String,
    onModeSelected: (EntryUpdateMode) -> Unit,
    onReleaseEstimateClicked: (() -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = MaterialTheme.padding.medium),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(MR.strings.entry_updates_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            if (mode == EntryUpdateMode.FOLLOW_RULES) {
                DecisionCard(text = decisionText(status, categories))
            }
            if (entry.updateStrategy == EntryUpdateStrategy.ONLY_FETCH_ONCE) {
                DecisionCard(text = stringResource(MR.strings.entry_updates_fetch_once))
            }
            EntryUpdateMode.entries.forEach { option ->
                ModeOption(
                    mode = option,
                    selected = option == mode,
                    onClick = { onModeSelected(option) },
                )
            }
            if (onReleaseEstimateClicked != null) {
                NavigationItem(
                    label = stringResource(MR.strings.entry_updates_release_estimate),
                    subtitle = releaseEstimate,
                    onClick = onReleaseEstimateClicked,
                )
            } else {
                Text(
                    text = releaseEstimate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DecisionCard(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ModeOption(mode: EntryUpdateMode, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(end = 12.dp))
        Column {
            Text(text = stringResource(mode.titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(mode.summaryRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
