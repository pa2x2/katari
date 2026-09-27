package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslationIssue
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shows the engine's disclosure and accepts it for translations on the page. */
@Composable
internal fun MangaPageTranslationDisclosureDialog(
    issue: MangaPageTranslationIssue.DisclosureRequired,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(issue.disclosure.title) },
        text = { Text(issue.disclosure.message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(issue.disclosure.confirmationLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )
}
