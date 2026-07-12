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
    presentation: EntryTypePresentation,
) {
    val options = buildList {
        add(DownloadAction.NEXT_1_CHAPTER to pluralStringResource(presentation.downloadAmountPlural, 1, 1))
        add(DownloadAction.NEXT_5_CHAPTERS to pluralStringResource(presentation.downloadAmountPlural, 5, 5))
        add(DownloadAction.NEXT_10_CHAPTERS to pluralStringResource(presentation.downloadAmountPlural, 10, 10))
        add(DownloadAction.NEXT_25_CHAPTERS to pluralStringResource(presentation.downloadAmountPlural, 25, 25))
        add(DownloadAction.UNREAD_CHAPTERS to stringResource(presentation.downloadUnconsumedLabel))
        if (presentation.downloadBookmarkedSupported) {
            add(DownloadAction.BOOKMARKED_CHAPTERS to stringResource(MR.strings.download_bookmarked))
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
