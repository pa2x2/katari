package mihon.entry.interactions.book.document.reader

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.resource.PendingBookRemoteResourceConsent
import mihon.entry.interactions.book.preparation.BookRemoteResourceRequest
import mihon.entry.interactions.book.preparation.BookRemoteResourceType

@Composable
internal fun BookDocumentExternalLinkDialog(
    host: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.book_document_external_link_title)) },
        text = {
            Text(stringResource(R.string.book_document_external_link_message, host))
        },
        confirmButton = {
            TextButton(onClick = onOpen) {
                Text(stringResource(R.string.book_document_external_link_open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.book_document_external_link_cancel))
            }
        },
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
    AlertDialog(
        onDismissRequest = onBlock,
        title = { Text(stringResource(R.string.book_remote_resources_title)) },
        text = {
            Text(
                pending.requests
                    .sortedWith(compareBy(BookRemoteResourceRequest::origin, BookRemoteResourceRequest::type))
                    .joinToString("\n") { request ->
                        val type = when (request.type) {
                            BookRemoteResourceType.IMAGE -> imageLabel
                            BookRemoteResourceType.FONT -> fontLabel
                        }
                        "• ${request.origin} — $type"
                    },
            )
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.book_remote_resources_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onBlock) {
                Text(stringResource(R.string.book_remote_resources_block))
            }
        },
    )
}
