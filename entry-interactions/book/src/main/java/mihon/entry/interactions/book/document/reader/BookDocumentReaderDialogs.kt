package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.resource.PendingBookRemoteResourceConsent
import mihon.entry.interactions.book.preparation.BookRemoteResourceRequest
import mihon.entry.interactions.book.preparation.BookRemoteResourceType
import mihon.presentation.kit.dialog.ConfirmDialog

@Composable
internal fun BookDocumentExternalLinkDialog(
    host: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(R.string.book_document_external_link_title),
        text = stringResource(R.string.book_document_external_link_message, host),
        confirmLabel = stringResource(R.string.book_document_external_link_open),
        onConfirm = onOpen,
        onDismissRequest = onDismiss,
        dismissLabel = stringResource(R.string.book_document_external_link_cancel),
    )
}

@Composable
internal fun BookRemoteResourceConsentDialog(
    pending: PendingBookRemoteResourceConsent,
    onAllow: () -> Unit,
    onBlock: () -> Unit,
) {
    val imageLabel = stringResource(R.string.book_remote_resource_images)
    val fontLabel = stringResource(R.string.book_remote_resource_fonts)
    ConfirmDialog(
        title = stringResource(R.string.book_remote_resources_title),
        text = pending.requests
            .sortedWith(compareBy(BookRemoteResourceRequest::origin, BookRemoteResourceRequest::type))
            .joinToString("\n") { request ->
                val type = when (request.type) {
                    BookRemoteResourceType.IMAGE -> imageLabel
                    BookRemoteResourceType.FONT -> fontLabel
                }
                "• ${request.origin} — $type"
            },
        confirmLabel = stringResource(R.string.book_remote_resources_allow),
        onConfirm = onAllow,
        onDismissRequest = onBlock,
        dismissLabel = stringResource(R.string.book_remote_resources_block),
    )
}
