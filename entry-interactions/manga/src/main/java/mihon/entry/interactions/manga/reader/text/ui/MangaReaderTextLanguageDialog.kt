package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Asks which language the text of a multi-language source is in. */
@Composable
internal fun MangaReaderTextLanguageDialog(
    languages: List<LanguageTag>,
    onChoose: (LanguageTag) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(MR.strings.reader_text_language_required)) },
        text = {
            Column {
                languages.forEach { language ->
                    ListItem(
                        modifier = Modifier.clickable { onChoose(language) },
                        content = { Text(language.displayName()) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
