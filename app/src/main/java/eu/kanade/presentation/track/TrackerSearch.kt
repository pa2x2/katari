package eu.kanade.presentation.track

import android.content.ClipData
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.launch
import mihon.entry.interactions.tracking.EntryTrackingSearchCandidate
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.icons.materialsymbols.rounded.OpenInBrowser
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.field.SearchField
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.sheet.SheetActions
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.runOnEnterKeyPressed

/**
 * Finds the tracker's record for an entry: the search field under the sheet header, results as cover rows in one card,
 * and Track (or Track privately) once one is selected. [title] names the tracker.
 */
@Composable
fun TrackerSearch(
    title: String,
    state: TextFieldState,
    onDispatchQuery: () -> Unit,
    queryResult: Result<List<EntryTrackingSearchCandidate>>?,
    selected: EntryTrackingSearchCandidate?,
    onSelectedChange: (EntryTrackingSearchCandidate) -> Unit,
    onConfirmSelection: (private: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    supportsPrivateTracking: Boolean,
) {
    val focusManager = LocalFocusManager.current
    val dispatchQueryAndClearFocus: () -> Unit = {
        onDispatchQuery()
        focusManager.clearFocus()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SheetHeader(title = title)
        SearchField(
            state = state,
            placeholder = stringResource(MR.strings.action_search_hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s)
                .runOnEnterKeyPressed(action = dispatchQueryAndClearFocus),
            onSearch = dispatchQueryAndClearFocus,
        )
        val results = queryResult?.getOrNull()
        when {
            queryResult == null -> LoadingScreen(modifier = Modifier.weight(1f, fill = false))
            results == null -> EmptyScreen(
                message = queryResult.exceptionOrNull()?.message ?: stringResource(MR.strings.unknown_error),
                modifier = Modifier.weight(1f, fill = false),
            )
            results.isEmpty() -> EmptyScreen(
                stringRes = MR.strings.no_results_found,
                modifier = Modifier.weight(1f, fill = false),
            )
            else -> RowList(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(vertical = KatariTheme.spacing.s),
            ) {
                itemsIndexed(items = results, key = { _, result -> result.hashCode() }) { index, result ->
                    RowGroupItem(
                        position = RowGroupPosition.of(index, results.size),
                        selected = result == selected,
                    ) {
                        SearchResultItem(
                            trackSearch = result,
                            onClick = {
                                focusManager.clearFocus()
                                onSelectedChange(result)
                            },
                        )
                    }
                }
            }
        }
        SheetActions(
            confirm = DialogAction(
                label = stringResource(MR.strings.action_track),
                onClick = { onConfirmSelection(false) },
                enabled = selected != null,
            ),
            dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
            neutral = DialogAction(
                label = stringResource(MR.strings.action_toggle_private_on),
                onClick = { onConfirmSelection(true) },
                enabled = selected != null,
            ).takeIf { supportsPrivateTracking },
        )
    }
}

@Composable
private fun SearchResultItem(
    trackSearch: EntryTrackingSearchCandidate,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard: Clipboard = LocalClipboard.current
    val locale = LocalLocale.current
    val type = trackSearch.publishingType.toLowerCase(locale).capitalize(locale)
    val status = trackSearch.publishingStatus.toLowerCase(locale).capitalize(locale)
    val description = trackSearch.summary.trim()
    var dropDownMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box {
        CoverRow(
            cover = {
                EntryCover.Book(
                    data = trackSearch.coverUrl,
                    modifier = Modifier.fillMaxSize(),
                    frame = CoverFrame.Thumbnail,
                )
            },
            onClick = onClick,
            onLongClick = { dropDownMenuExpanded = true },
        ) {
            Text(
                text = trackSearch.title,
                role = TextRole.ItemTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val details = listOfNotNull(
                (trackSearch.authors + trackSearch.artists).distinct().joinToString().takeIf { it.isNotBlank() },
                type.takeIf { it.isNotBlank() },
                status.takeIf { it.isNotBlank() },
                trackSearch.publicationStartDate.takeIf { it.isNotBlank() },
                trackSearch.score.takeIf {
                    it != -1.0
                }?.let { stringResource(MR.strings.track_search_score, it.toString()) },
            )
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    role = TextRole.Supporting,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    role = TextRole.Supporting,
                    tone = ContentTone.Secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        SearchResultItemDropDownMenu(
            expanded = dropDownMenuExpanded,
            onCollapseMenu = { dropDownMenuExpanded = false },
            onCopyName = {
                scope.launch {
                    clipboard.setClipEntry(ClipData.newPlainText(trackSearch.title, trackSearch.title).toClipEntry())
                }
            },
            onOpenInBrowser = {
                val url = trackSearch.remoteUrl
                if (url.isNotBlank()) {
                    context.openInBrowser(url)
                }
            },
        )
    }
}

@Composable
private fun SearchResultItemDropDownMenu(
    expanded: Boolean,
    onCollapseMenu: () -> Unit,
    onCopyName: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onCollapseMenu,
    ) {
        DropdownMenuItem(
            label = stringResource(MR.strings.action_copy_to_clipboard),
            onClick = onCopyName,
            icon = MaterialSymbols.Rounded.ContentCopy,
        )
        DropdownMenuItem(
            label = stringResource(MR.strings.action_open_in_browser),
            onClick = onOpenInBrowser,
            icon = MaterialSymbols.Rounded.OpenInBrowser,
        )
    }
}

@PreviewLightDark
@Composable
private fun TrackerSearchPreviews(
    @PreviewParameter(TrackerSearchPreviewProvider::class)
    content: @Composable () -> Unit,
) {
    KatariPreviewTheme { content() }
}
