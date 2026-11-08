package app.katari.entry.interactions.book.document.reader.links

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import app.katari.book.api.document.BookDocumentLink
import app.katari.book.api.document.BookDocumentLinkTarget
import app.katari.entry.interactions.book.document.reader.LocalBookDocumentTextInteraction
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ContentCopy
import app.katari.icons.materialsymbols.rounded.OpenInBrowser
import app.katari.presentation.kit.menu.ContextMenu
import app.katari.presentation.kit.menu.DropdownMenuItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** What a right click on a web link offers, at [position] in the box around its text. */
@Composable
internal fun BoxScope.BookDocumentLinkMenu(url: String, position: Offset, onDismissRequest: () -> Unit) {
    val interaction = LocalBookDocumentTextInteraction.current
    val copyLabel = stringResource(MR.strings.action_copy_link)
    ContextMenu(position = position, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            label = stringResource(MR.strings.action_open_in_browser),
            icon = MaterialSymbols.Rounded.OpenInBrowser,
            onClick = { interaction.openLink(url) },
        )
        DropdownMenuItem(
            label = copyLabel,
            icon = MaterialSymbols.Rounded.ContentCopy,
            onClick = { interaction.copyText(copyLabel, url) },
        )
    }
}

/**
 * The address of the web link drawn at [position] in this text, whose links are [links] in the order they were added
 * as annotations; null over anything else, a link inside the book among them.
 */
internal fun TextLayoutResult.webLinkAt(position: Offset, links: List<BookDocumentLink>): String? {
    val text = layoutInput.text
    // The nearest caret position can fall after the character under the pointer
    val caret = getOffsetForPosition(position)
    val character = listOf(caret, caret - 1).firstOrNull { it in text.indices && getBoundingBox(it).contains(position) }
        ?: return null
    return text.getLinkAnnotations(character, character + 1).firstNotNullOfOrNull { range ->
        val index = (range.item as? LinkAnnotation.Clickable)?.tag?.removePrefix(BOOK_DOCUMENT_LINK_TAG_PREFIX)
            ?.toIntOrNull()
        (index?.let(links::getOrNull)?.target as? BookDocumentLinkTarget.External)?.url
    }
}

/** Tags a link's annotation, followed by where the link is in the text's links. */
internal const val BOOK_DOCUMENT_LINK_TAG_PREFIX = "book-document-link-"
