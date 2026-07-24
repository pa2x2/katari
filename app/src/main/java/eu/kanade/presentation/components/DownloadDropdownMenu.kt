package eu.kanade.presentation.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.EntryTypePresentation
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.i18n.MR
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
    offset: DpOffset? = null,
) {
    if (offset != null) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            offset = offset,
            content = {
                DownloadDropdownMenuItems(
                    onDismissRequest = onDismissRequest,
                    onDownloadClicked = onDownloadClicked,
                    bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
                    presentation = presentation,
                )
            },
        )
    } else {
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
}

@Composable
private fun DownloadDropdownMenuItems(
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    bookmarkedDownloadsSupported: Boolean,
    presentation: EntryTypePresentation,
) {
    val options = downloadActions(bookmarkedDownloadsSupported).map { action ->
        action to when (action) {
            DownloadAction.NEXT_1_CHAPTER -> pluralStringResource(presentation.downloadAmountPlural, 1, 1)
            DownloadAction.NEXT_5_CHAPTERS -> pluralStringResource(presentation.downloadAmountPlural, 5, 5)
            DownloadAction.NEXT_10_CHAPTERS -> pluralStringResource(presentation.downloadAmountPlural, 10, 10)
            DownloadAction.NEXT_25_CHAPTERS -> pluralStringResource(presentation.downloadAmountPlural, 25, 25)
            DownloadAction.UNREAD_CHAPTERS -> stringResource(presentation.downloadUnconsumedLabel)
            DownloadAction.BOOKMARKED_CHAPTERS -> stringResource(MR.strings.download_bookmarked)
        }
    }

    options.map { (downloadAction, string) ->
        DropdownMenuItem(
            text = { Text(text = string) },
            onClick = {
                onDownloadClicked(downloadAction)
                onDismissRequest()
            },
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
