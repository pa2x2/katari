package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.reader_text_translate_downloads_offer_title)) },
        text = { Text(pluralStringResource(MR.plurals.reader_text_translate_downloads_offer, count, count)) },
        confirmButton = {
            TextButton(onClick = onTranslate) { Text(stringResource(MR.strings.action_translate)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(MR.strings.action_not_now)) }
        },
    )
}
