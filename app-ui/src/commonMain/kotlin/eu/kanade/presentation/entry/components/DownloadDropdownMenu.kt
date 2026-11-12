package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Devices
import app.katari.icons.materialsymbols.rounded.Download
import app.katari.presentation.kit.menu.DropdownMenu
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.selection.ItemAction
import app.katari.presentation.kit.sheet.PopoverAnchor
import app.katari.sync.ui.download.DownloadElsewhereAmount
import app.katari.sync.ui.download.DownloadElsewhereRequest
import app.katari.sync.ui.download.LocalDownloadElsewhere
import app.katari.sync.ui.download.amountsLabel
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.EntryDownloadElsewhere
import eu.kanade.presentation.entry.EntryTypePresentation
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An entry's Download menu: how much to download, then, where another device of the account can take the entry, the
 * same amounts on that device ([downloadElsewhere]), picked in a popover beside [anchor]'s button.
 */
@Composable
fun DownloadDropdownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onDownloadClicked: (DownloadAction) -> Unit,
    bookmarkedDownloadsSupported: Boolean,
    presentation: EntryTypePresentation = null.entryTypePresentation(),
    downloadElsewhere: EntryDownloadElsewhere? = null,
    anchor: PopoverAnchor? = null,
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
            if (downloadElsewhere != null) {
                DownloadElsewhereMenuItem(
                    downloadElsewhere = downloadElsewhere,
                    anchor = anchor,
                    bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
                    presentation = presentation,
                    onDismissRequest = onDismissRequest,
                )
            }
        },
    )
}

@Composable
private fun DownloadElsewhereMenuItem(
    downloadElsewhere: EntryDownloadElsewhere,
    anchor: PopoverAnchor?,
    bookmarkedDownloadsSupported: Boolean,
    presentation: EntryTypePresentation,
    onDismissRequest: () -> Unit,
) {
    val elsewhere = LocalDownloadElsewhere.current ?: return
    val offer = elsewhere.offer(downloadElsewhere.entryIds) ?: return
    val amounts = downloadActions(bookmarkedDownloadsSupported).map { action ->
        DownloadElsewhereAmount(action.label(presentation)) { downloadElsewhere.chapters(action) }
    }
    DropdownMenuItem(
        label = offer.amountsLabel(),
        onClick = {
            onDismissRequest()
            elsewhere.open(
                DownloadElsewhereRequest.Amounts(
                    entryIds = downloadElsewhere.entryIds,
                    sourceId = downloadElsewhere.sourceId,
                    subject = downloadElsewhere.title,
                    amounts = amounts,
                ),
                anchor,
            )
        },
        icon = MaterialSymbols.Rounded.Devices,
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
            label = action.label(presentation),
            icon = MaterialSymbols.Rounded.Download,
            onClick = { onDownloadClicked(action) },
        )
    }
}

@Composable
private fun DownloadAction.label(presentation: EntryTypePresentation): String = when (this) {
    DownloadAction.NEXT_1_CHAPTER -> pluralStringResource(presentation.downloadAmountPlural, 1, 1)
    DownloadAction.NEXT_5_CHAPTERS -> pluralStringResource(presentation.downloadAmountPlural, 5, 5)
    DownloadAction.NEXT_10_CHAPTERS -> pluralStringResource(presentation.downloadAmountPlural, 10, 10)
    DownloadAction.NEXT_25_CHAPTERS -> pluralStringResource(presentation.downloadAmountPlural, 25, 25)
    DownloadAction.UNREAD_CHAPTERS -> stringResource(presentation.downloadUnconsumedLabel)
    DownloadAction.BOOKMARKED_CHAPTERS -> stringResource(MR.strings.download_bookmarked)
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
