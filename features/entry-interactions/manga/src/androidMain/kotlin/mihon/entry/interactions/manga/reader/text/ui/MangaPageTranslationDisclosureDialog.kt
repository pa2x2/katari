package mihon.entry.interactions.manga.reader.text.ui

import androidx.compose.runtime.Composable
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslationIssue
import mihon.presentation.kit.dialog.ConfirmDialog

/** Shows the engine's disclosure and accepts it for translations on the page. */
@Composable
internal fun MangaPageTranslationDisclosureDialog(
    issue: MangaPageTranslationIssue.DisclosureRequired,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = issue.disclosure.title,
        text = issue.disclosure.message,
        confirmLabel = issue.disclosure.confirmationLabel,
        onConfirm = onConfirm,
        onDismissRequest = onDismiss,
    )
}
