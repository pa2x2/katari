package app.katari.entry.interactions.book.document.reader.picture

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import app.katari.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import app.katari.entry.interactions.book.preparation.BookPublicationResourceLoader
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ContentCopy
import app.katari.icons.materialsymbols.rounded.OpenInFull
import app.katari.icons.materialsymbols.rounded.Save
import app.katari.presentation.kit.menu.ContextMenu
import app.katari.presentation.kit.menu.DropdownMenuItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * What a right click on a book's picture offers, at [position] in the box around it: opening it full screen, and
 * saving or copying the book's file of it through [handOn], which outlives the menu as saving goes on after it closes.
 */
@Composable
internal fun BoxScope.BookDocumentPictureMenu(
    resourceId: String,
    resourceLoader: BookPublicationResourceLoader,
    handOn: BookDocumentPictureHandOn,
    position: Offset,
    onOpen: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val bytes: suspend () -> ByteArray = {
        resourceLoader.load(
            resourceId = resourceId,
            acceptedMediaTypes = PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
            maxBytes = PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
        ).getOrThrow().bytes
    }
    ContextMenu(position = position, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            label = stringResource(MR.strings.book_reader_open_picture),
            icon = MaterialSymbols.Rounded.OpenInFull,
            onClick = onOpen,
        )
        if (handOn.export.offersSaving) {
            DropdownMenuItem(
                label = stringResource(MR.strings.action_save),
                icon = MaterialSymbols.Rounded.Save,
                onClick = { handOn.save(resourceId, bytes) },
            )
        }
        if (handOn.export.offersCopying) {
            DropdownMenuItem(
                label = stringResource(MR.strings.action_copy),
                icon = MaterialSymbols.Rounded.ContentCopy,
                onClick = { handOn.copy(resourceId, bytes) },
            )
        }
    }
}
