package eu.kanade.tachiyomi.ui.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import dev.icerock.moko.resources.StringResource
import mihon.entry.interactions.download.EntryDownloadPhase
import mihon.entry.interactions.download.EntryDownloadQueueItem
import mihon.entry.interactions.download.description
import mihon.entry.interactions.download.resolve
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Cancel
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.VerticalAlignBottom
import mihon.icons.materialsymbols.rounded.VerticalAlignTop
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuDivider
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One queued download: the series, then its chapter and state, with a bar while it transfers. [handleModifier] makes
 * the leading handle drag the row within its source group.
 */
@Composable
internal fun DownloadQueueRow(
    item: EntryDownloadQueueItem,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    handleModifier: Modifier,
    onMove: (moveToTop: Boolean) -> Unit,
    onMoveSeries: (moveToTop: Boolean) -> Unit,
    onCancel: () -> Unit,
    onCancelSeries: () -> Unit,
) {
    val context = LocalContext.current
    val presentation = item.presentation
    val status = presentation.description()?.resolve(context)
    SettingsRow(
        title = item.title,
        leading = {
            Icon(
                imageVector = MaterialSymbols.Rounded.DragHandle,
                contentDescription = null,
                tone = ContentTone.Secondary,
                modifier = handleModifier,
            )
        },
        trailing = {
            RowMenu(
                isFirstInGroup = isFirstInGroup,
                isLastInGroup = isLastInGroup,
                onMove = onMove,
                onMoveSeries = onMoveSeries,
                onCancel = onCancel,
                onCancelSeries = onCancelSeries,
            )
        },
        below = {
            Text(
                text = listOfNotNull(item.subtitle, status).joinToString(" · "),
                role = TextRole.Supporting,
                tone = if (presentation.phase == EntryDownloadPhase.FAILED) ContentTone.Error else ContentTone.Inherit,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            when (presentation.phase) {
                EntryDownloadPhase.TRANSFERRING -> LinearProgressIndicator(
                    modifier = Modifier.padding(top = KatariTheme.spacing.s),
                    progress = { item.progress.toFloat() / item.progressMax },
                )
                EntryDownloadPhase.RESOLVING, EntryDownloadPhase.FINALIZING -> LinearProgressIndicator(
                    modifier = Modifier.padding(top = KatariTheme.spacing.s),
                )
                else -> Unit
            }
        },
    )
}

@Composable
private fun RowMenu(
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onMove: (moveToTop: Boolean) -> Unit,
    onMoveSeries: (moveToTop: Boolean) -> Unit,
    onCancel: () -> Unit,
    onCancelSeries: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            icon = MaterialSymbols.Rounded.MoreVert,
            contentDescription = stringResource(MR.strings.action_menu_overflow_description),
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val entries = listOfNotNull(
                QueueRowAction(MR.strings.action_move_to_top, MaterialSymbols.Rounded.VerticalAlignTop) {
                    onMove(true)
                }.takeUnless { isFirstInGroup },
                QueueRowAction(MR.strings.action_move_to_top_all_for_series, MaterialSymbols.Rounded.VerticalAlignTop) {
                    onMoveSeries(true)
                },
                QueueRowAction(MR.strings.action_move_to_bottom, MaterialSymbols.Rounded.VerticalAlignBottom) {
                    onMove(false)
                }.takeUnless { isLastInGroup },
                QueueRowAction(
                    MR.strings.action_move_to_bottom_all_for_series,
                    MaterialSymbols.Rounded.VerticalAlignBottom,
                ) { onMoveSeries(false) },
                QueueRowAction(MR.strings.action_cancel, MaterialSymbols.Rounded.Close, onCancel),
                QueueRowAction(MR.strings.cancel_all_for_series, MaterialSymbols.Rounded.Cancel, onCancelSeries),
            )
            entries.forEachIndexed { index, entry ->
                // Moving and cancelling are split into their own groups.
                if (index > 0 && entry.label == MR.strings.action_cancel) DropdownMenuDivider()
                DropdownMenuItem(label = stringResource(entry.label), onClick = entry.action, icon = entry.icon)
            }
        }
    }
}

private class QueueRowAction(val label: StringResource, val icon: ImageVector, val action: () -> Unit)
