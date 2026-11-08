package eu.kanade.presentation.reader.appbars

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.source.EntryChildWebViewAction
import app.katari.entry.interactions.source.EntryChildWebViewActionItems
import app.katari.entry.interactions.source.EntryChildWebViewResolution
import app.katari.entry.viewer.chrome.ui.bars.ReaderChromeTopBar
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Bookmark
import app.katari.icons.materialsymbols.rounded.SwapHoriz
import app.katari.icons.materialsymbols.roundedfilled.Bookmark
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.menu.DropdownMenuDivider
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.menu.OverflowMenu
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ReaderTopBar(
    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    /** The web page actions the platform can carry out. */
    webViewActions: List<EntryChildWebViewAction>,
    /** Null while one page shows at a time. */
    onShiftPages: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ReaderChromeTopBar(
        modifier = modifier,
        title = mangaTitle,
        subtitle = chapterTitle,
        navigateUp = navigateUp,
        actions = {
            IconButton(
                icon = if (bookmarked) {
                    MaterialSymbols.RoundedFilled.Bookmark
                } else {
                    MaterialSymbols.Rounded.Bookmark
                },
                contentDescription = stringResource(
                    if (bookmarked) {
                        MR.strings.action_remove_bookmark
                    } else {
                        MR.strings.action_bookmark
                    },
                ),
                onClick = onToggleBookmarked,
            )

            if (childWebView != null || onShiftPages != null) {
                OverflowMenu { close ->
                    childWebView?.let { EntryChildWebViewActionItems(it, onChildWebViewAction, close, webViewActions) }
                    if (onShiftPages != null) {
                        if (childWebView != null) DropdownMenuDivider()
                        DropdownMenuItem(
                            label = stringResource(MR.strings.action_shift_pages),
                            icon = MaterialSymbols.Rounded.SwapHoriz,
                            onClick = {
                                close()
                                onShiftPages()
                            },
                        )
                    }
                }
            }
        },
    )
}
