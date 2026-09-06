package mihon.entry.interactions.book.format.html.prosechapter.parsing

import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentStyle
import org.jsoup.nodes.Element

/** Preserve quoted block boundaries so paragraphs and their links remain independently laid out. */
internal fun HtmlProseBlockParser.addQuoteBlocks(
    element: Element,
    style: BookDocumentStyle,
    noteContext: Boolean,
    inheritedFragments: List<String>,
    destination: MutableList<HtmlProseParsedBlock>,
): Boolean {
    val nested = collectNested(element, style, noteContext)
    nested.forEachIndexed { index, block ->
        val fragments = if (index == 0) (inheritedFragments + block.fragments).distinct() else block.fragments
        destination += block.copy(
            role = if (block.role.kind == BookDocumentBlockKind.PARAGRAPH) {
                BookDocumentBlockRole(BookDocumentBlockKind.QUOTE)
            } else {
                block.role
            },
            fragments = fragments,
            explicitId = block.explicitId ?: fragments.firstOrNull(),
        )
    }
    return nested.isNotEmpty()
}
