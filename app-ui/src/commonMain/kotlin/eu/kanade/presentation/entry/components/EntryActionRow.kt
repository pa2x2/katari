package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.OpenInBrowser
import app.katari.icons.materialsymbols.rounded.Update
import app.katari.icons.materialsymbols.roundedfilled.Warning
import app.katari.presentation.kit.button.ActionButton
import app.katari.presentation.kit.button.ActionButtonRow
import app.katari.presentation.kit.sheet.PopoverAnchor
import app.katari.presentation.kit.sheet.popoverAnchor
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.presentation.entry.updates.EntryUpdatesLabel
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The row of actions under an entry's header; an action is coloured while what it controls is in effect. */
@Composable
fun EntryActionRow(
    favorite: Boolean,
    updatesLabel: EntryUpdatesLabel,
    onAddToLibraryClicked: () -> Unit,
    onAddToMergeClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onDuplicatesClicked: (() -> Unit)?,
    onUpdatesClicked: (() -> Unit)?,
    updatesAnchor: PopoverAnchor,
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
            modifier = Modifier.popoverAnchor(updatesAnchor),
        )
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
