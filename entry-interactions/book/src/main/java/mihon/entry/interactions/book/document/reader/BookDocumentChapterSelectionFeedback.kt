package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.feedback.LoadingScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Feedback for a chapter opened from the contents or the seek controls. Such a chapter is often
 * not adjacent to the current one, so no transition row exists to show its loading or failure.
 * Loading replaces the page the reader is leaving; a failure keeps that page and offers a retry.
 */
@Composable
internal fun BookDocumentChapterSelectionFeedback(
    selection: BookDocumentChapterSelectionLoad,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (val loadState = selection.loadState) {
        BookDocumentChapterLoadState.Loading -> {
            val description = stringResource(MR.strings.loading)
            LoadingScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalBookDocumentReaderPalette.current.background)
                    .semantics { contentDescription = description },
            )
        }
        is BookDocumentChapterLoadState.Failed -> ConfirmDialog(
            title = selection.chapter.name,
            text = loadState.message,
            confirmLabel = stringResource(MR.strings.action_retry),
            onConfirm = onRetry,
            onDismissRequest = onDismiss,
        )
    }
}
