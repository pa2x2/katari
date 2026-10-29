package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.runtime.Composable
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Offers to translate the series' chapters downloaded before its downloads were set to be translated. */
@Composable
internal fun MangaTranslateDownloadsOfferDialog(
    count: Int,
    onTranslate: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.reader_text_translate_downloads_offer_title),
        text = pluralStringResource(MR.plurals.reader_text_translate_downloads_offer, count, count),
        confirmLabel = stringResource(MR.strings.action_translate),
        onConfirm = onTranslate,
        onDismissRequest = onDismiss,
        dismissLabel = stringResource(MR.strings.action_not_now),
    )
}
