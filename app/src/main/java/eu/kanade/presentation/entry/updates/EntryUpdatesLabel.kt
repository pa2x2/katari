package eu.kanade.presentation.entry.updates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import tachiyomi.domain.library.update.model.EntryUpdateMode
import tachiyomi.domain.library.update.model.EntryUpdateOutcome
import tachiyomi.domain.library.update.model.EntryUpdateStatus
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** What the entry's Updates button says, and how loudly. */
data class EntryUpdatesLabel(val text: String, val emphasis: Emphasis) {
    enum class Emphasis { Quiet, Highlighted, Error }
}

/**
 * The most pressing thing about the entry's updates: a fix it needs, a mode set on it, what the latest update did with
 * it, and otherwise when its next release is expected.
 *
 * @param nextUpdateDays days until the predicted release, or null when there is no prediction.
 */
@Composable
@ReadOnlyComposable
fun entryUpdatesLabel(
    mode: EntryUpdateMode,
    status: EntryUpdateStatus?,
    nextUpdateDays: Int?,
    isUserInterval: Boolean,
): EntryUpdatesLabel {
    return when {
        status?.isFailingRepeatedly == true ->
            EntryUpdatesLabel(stringResource(MR.strings.entry_updates_label_failing), EntryUpdatesLabel.Emphasis.Error)
        mode == EntryUpdateMode.NEVER ->
            EntryUpdatesLabel(
                stringResource(MR.strings.entry_updates_label_paused),
                EntryUpdatesLabel.Emphasis.Highlighted,
            )
        mode == EntryUpdateMode.ALWAYS ->
            EntryUpdatesLabel(
                stringResource(MR.strings.entry_updates_label_always),
                EntryUpdatesLabel.Emphasis.Highlighted,
            )
        status?.outcome == EntryUpdateOutcome.SKIPPED ->
            EntryUpdatesLabel(stringResource(MR.strings.entry_updates_label_skipped), EntryUpdatesLabel.Emphasis.Quiet)
        status?.outcome == EntryUpdateOutcome.NOT_CHECKED ->
            EntryUpdatesLabel(
                stringResource(MR.strings.entry_updates_label_not_checked),
                EntryUpdatesLabel.Emphasis.Quiet,
            )
        else -> EntryUpdatesLabel(
            text = when (nextUpdateDays) {
                null -> stringResource(MR.strings.entry_updates_title)
                0 -> stringResource(MR.strings.manga_interval_expected_update_soon)
                else -> pluralStringResource(MR.plurals.day, count = nextUpdateDays, nextUpdateDays)
            },
            emphasis = if (isUserInterval) EntryUpdatesLabel.Emphasis.Highlighted else EntryUpdatesLabel.Emphasis.Quiet,
        )
    }
}
