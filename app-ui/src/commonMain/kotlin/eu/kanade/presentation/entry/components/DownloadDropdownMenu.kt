package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Download
import app.katari.presentation.kit.menu.DropdownMenu
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.selection.ItemAction
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.EntryTypePresentation
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DownloadDropdownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    bookmarkedDownloadsSupported: Boolean,
    presentation: EntryTypePresentation = null.entryTypePresentation(),
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        content = {
            DownloadDropdownMenuItems(
                onDismissRequest = onDismissRequest,
                onDownloadClicked = onDownloadClicked,
                bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
                presentation = presentation,
            )
        },
    )
}

@Composable
private fun DownloadDropdownMenuItems(
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    bookmarkedDownloadsSupported: Boolean,
    presentation: EntryTypePresentation,
) {
    downloadChoices(onDownloadClicked, bookmarkedDownloadsSupported, presentation).forEach { choice ->
        DropdownMenuItem(
            label = choice.label,
            onClick = {
                choice.onClick()
                onDismissRequest()
            },
            icon = choice.icon,
        )
    }
}

/** How much to download, as the choices of a Download action. */
@Composable
fun downloadChoices(
    onDownloadClicked: (DownloadAction) -> Unit,
    bookmarkedDownloadsSupported: Boolean,
    presentation: EntryTypePresentation = null.entryTypePresentation(),
): List<ItemAction> {
    return downloadActions(bookmarkedDownloadsSupported).map { action ->
        ItemAction(
            label = when (action) {
                DownloadAction.NEXT_1_CHAPTER -> pluralStringResource(presentation.downloadAmountPlural, 1, 1)
                DownloadAction.NEXT_5_CHAPTERS -> pluralStringResource(presentation.downloadAmountPlural, 5, 5)
                DownloadAction.NEXT_10_CHAPTERS -> pluralStringResource(presentation.downloadAmountPlural, 10, 10)
                DownloadAction.NEXT_25_CHAPTERS -> pluralStringResource(presentation.downloadAmountPlural, 25, 25)
                DownloadAction.UNREAD_CHAPTERS -> stringResource(presentation.downloadUnconsumedLabel)
                DownloadAction.BOOKMARKED_CHAPTERS -> stringResource(MR.strings.download_bookmarked)
            },
            icon = MaterialSymbols.Rounded.Download,
            onClick = { onDownloadClicked(action) },
        )
    }
}

internal fun downloadActions(bookmarkedDownloadsSupported: Boolean): List<DownloadAction> {
    return buildList {
        add(DownloadAction.NEXT_1_CHAPTER)
        add(DownloadAction.NEXT_5_CHAPTERS)
        add(DownloadAction.NEXT_10_CHAPTERS)
        add(DownloadAction.NEXT_25_CHAPTERS)
        add(DownloadAction.UNREAD_CHAPTERS)
        if (bookmarkedDownloadsSupported) {
            add(DownloadAction.BOOKMARKED_CHAPTERS)
        }
    }
}
