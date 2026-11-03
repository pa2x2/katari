package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.presentation.entry.updates.EntryUpdatesLabel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.icons.materialsymbols.rounded.Sync
import mihon.icons.materialsymbols.rounded.Update
import mihon.icons.materialsymbols.roundedfilled.CheckCircle
import mihon.icons.materialsymbols.roundedfilled.Warning
import mihon.presentation.kit.button.ActionButton
import mihon.presentation.kit.button.ActionButtonRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** The row of actions under an entry's header; an action is coloured while what it controls is in effect. */
@Composable
fun EntryActionRow(
    favorite: Boolean,
    trackingCount: Int,
    updatesLabel: EntryUpdatesLabel,
    onAddToLibraryClicked: () -> Unit,
    onAddToMergeClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onDuplicatesClicked: (() -> Unit)?,
    onUpdatesClicked: (() -> Unit)?,
    onEditCategory: (() -> Unit)?,
    modifier: Modifier = Modifier,
    wrap: Boolean = false,
) {
    ActionButtonRow(modifier = modifier.padding(top = KatariTheme.spacing.s), wrap = wrap) {
        ActionButton(
            label = if (favorite) {
                stringResource(MR.strings.in_library)
            } else {
                stringResource(MR.strings.add_to_library)
            },
            icon = EntryActionIcons.library(favorite),
            tone = if (favorite) ContentTone.Primary else QuietTone,
            onClick = onAddToLibraryClicked,
            onLongClick = onEditCategory,
        )
        if (onAddToMergeClicked != null) {
            ActionButton(
                label = stringResource(MR.strings.action_add_to_merge),
                icon = EntryActionIcons.merge,
                tone = QuietTone,
                onClick = onAddToMergeClicked,
            )
        }
        ActionButton(
            label = updatesLabel.text,
            icon = MaterialSymbols.Rounded.Update,
            tone = when (updatesLabel.emphasis) {
                EntryUpdatesLabel.Emphasis.Quiet -> QuietTone
                EntryUpdatesLabel.Emphasis.Highlighted -> ContentTone.Primary
                EntryUpdatesLabel.Emphasis.Error -> ContentTone.Error
            },
            onClick = { onUpdatesClicked?.invoke() },
        )
        if (onTrackingClicked != null) {
            ActionButton(
                label = if (trackingCount == 0) {
                    stringResource(MR.strings.manga_tracking_tab)
                } else {
                    pluralStringResource(MR.plurals.num_trackers, count = trackingCount, trackingCount)
                },
                icon = if (trackingCount == 0) {
                    MaterialSymbols.Rounded.Sync
                } else {
                    MaterialSymbols.RoundedFilled.CheckCircle
                },
                tone = if (trackingCount == 0) QuietTone else ContentTone.Primary,
                onClick = onTrackingClicked,
            )
        }
        if (onWebViewClicked != null) {
            ActionButton(
                label = stringResource(MR.strings.action_web_view),
                icon = MaterialSymbols.Rounded.OpenInBrowser,
                tone = QuietTone,
                onClick = onWebViewClicked,
                onLongClick = onWebViewLongClicked,
            )
        }
        if (onDuplicatesClicked != null) {
            ActionButton(
                label = stringResource(MR.strings.action_duplicates),
                icon = MaterialSymbols.RoundedFilled.Warning,
                tone = ContentTone.Error,
                onClick = onDuplicatesClicked,
            )
        }
    }
}

// Actions whose state is not in effect stay readable but recede behind the coloured ones.
private val QuietTone = ContentTone.Secondary
