package eu.kanade.presentation.reader.appbars

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewActionsMenu
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.viewer.chrome.ui.ReaderChromeTopBar
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Bookmark
import mihon.icons.materialsymbols.roundedfilled.Bookmark
import mihon.presentation.kit.button.IconButton
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

            EntryChildWebViewActionsMenu(
                resolution = childWebView,
                onAction = onChildWebViewAction,
            )
        },
    )
}
