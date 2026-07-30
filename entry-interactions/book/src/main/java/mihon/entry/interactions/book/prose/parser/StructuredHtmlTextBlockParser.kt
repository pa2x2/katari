package mihon.entry.interactions.book.prose

import android.text.SpannableString
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import mihon.entry.interactions.book.document.model.BookDocumentWhiteSpace
import org.jsoup.nodes.Element

internal fun StructuredHtmlProseParser.addParagraph(
    element: Element,
    style: BookDocumentStyle,
    noteContext: Boolean,
    inheritedFragments: List<String>,
) {
    val pieces = element.splitAroundImages()
    if (pieces.none { it is ParagraphPiece.Image }) {
        addTextBlock(
            element,
            BookDocumentBlockRole(
                if (noteContext) BookDocumentBlockKind.NOTE else BookDocumentBlockKind.PARAGRAPH,
            ),
            style,
            inheritedFragments,
        )
        return
    }
    val paragraphFragments = element.ownFragments()
    var leadingIdentityAssigned = false
    pieces.forEach { piece ->
        val before = parsedBlocks.size
        val fragments = (inheritedFragments + paragraphFragments)
            .takeUnless { leadingIdentityAssigned }
            .orEmpty()
        when (piece) {
            is ParagraphPiece.Inline -> {
                piece.element.removeAttr("id")
                piece.element.removeAttr("name")
                addTextBlock(
                    piece.element,
                    BookDocumentBlockRole(
                        if (noteContext) BookDocumentBlockKind.NOTE else BookDocumentBlockKind.PARAGRAPH,
                    ),
                    style,
                    fragments,
                )
            }
            is ParagraphPiece.Image -> addImage(piece.element, style, fragments)
        }
        if (parsedBlocks.size > before && !leadingIdentityAssigned) {
            paragraphFragments.firstOrNull()?.let { explicitId ->
                parsedBlocks[before] = parsedBlocks[before].copy(explicitId = explicitId)
            }
            leadingIdentityAssigned = true
        }
    }
}

internal fun StructuredHtmlProseParser.addTextBlock(
    element: Element,
    role: BookDocumentBlockRole,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    if (!element.hasReadableText()) return
    val rendered = when (style.whiteSpace) {
        BookDocumentWhiteSpace.PRE,
        BookDocumentWhiteSpace.PRE_WRAP,
        -> renderPreservedText(element)
        BookDocumentWhiteSpace.NORMAL -> renderHtml(element)
    }
    if (rendered.text.none(Char::isReadableDocumentCharacter)) return
    val styled = rendered.text.withSemanticStyle(style)
    parsedBlocks += ParsedBlock(
        renderedText = styled,
        logicalPlainText = styled.toString().trim(),
        role = role,
        content = BookDocumentBlockContent.Text(
            preformatted = style.whiteSpace != BookDocumentWhiteSpace.NORMAL,
        ),
        style = style,
        explicitId = element.id().ifBlank { null },
        fragments = (inheritedFragments + element.fragments()).distinct(),
        localAnchorOffsets = rendered.anchorOffsets,
        inlineStyles = rendered.inlineStyles,
    )
}

internal fun StructuredHtmlProseParser.addPreformatted(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    val rendered = renderPreservedText(element).trimEnd().withParagraphTerminator()
    if (rendered.text.none(Char::isReadableDocumentCharacter)) return
    val preStyle = style.copy(
        whiteSpace = BookDocumentWhiteSpace.PRE,
        fontFamily = BookDocumentFontFamily.Generic(BookDocumentFontFamily.GenericFamily.MONOSPACE),
    )
    parsedBlocks += ParsedBlock(
        renderedText = rendered.text,
        logicalPlainText = rendered.text.toString().trim(),
        role = BookDocumentBlockRole(BookDocumentBlockKind.PREFORMATTED),
        content = BookDocumentBlockContent.Text(preformatted = true),
        style = preStyle,
        explicitId = element.id().ifBlank { null },
        fragments = (inheritedFragments + element.fragments()).distinct(),
        localAnchorOffsets = rendered.anchorOffsets,
    )
}

internal fun StructuredHtmlProseParser.addThematicBreak(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    parsedBlocks += ParsedBlock(
        renderedText = SpannableString(OBJECT_REPLACEMENT_TEXT),
        logicalPlainText = "",
        role = BookDocumentBlockRole(BookDocumentBlockKind.THEMATIC_BREAK),
        content = BookDocumentBlockContent.ThematicBreak,
        style = style,
        explicitId = element.id().ifBlank { null },
        fragments = (inheritedFragments + element.fragments()).distinct(),
    )
}

internal fun StructuredHtmlProseParser.addUnsupportedBlock(
    element: Element,
    style: BookDocumentStyle,
    inheritedFragments: List<String>,
) {
    val text = UNSUPPORTED_CONTENT_BLOCK_TEXT.withParagraphTerminator()
    parsedBlocks += ParsedBlock(
        renderedText = SpannableString(text),
        logicalPlainText = UNSUPPORTED_CONTENT_BLOCK_TEXT,
        role = BookDocumentBlockRole(BookDocumentBlockKind.UNSUPPORTED),
        content = BookDocumentBlockContent.Unsupported(
            element.attr("data-katari-unsupported").take(STRUCTURED_HTML_MAX_DIAGNOSTIC_LENGTH),
        ),
        style = style,
        explicitId = element.id().ifBlank { null },
        fragments = (inheritedFragments + element.fragments()).distinct(),
    )
}
