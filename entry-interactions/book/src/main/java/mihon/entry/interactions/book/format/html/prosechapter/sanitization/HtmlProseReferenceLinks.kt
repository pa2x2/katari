package mihon.entry.interactions.book.format.html.prosechapter.sanitization

import mihon.book.api.document.BookDocumentLinkTarget
import org.jsoup.nodes.Element

/** Recognizes explicit note semantics and the paired note/backlink IDs used in Gutenberg and Pandoc HTML. */
internal fun Element.contextualReferenceTarget(target: BookDocumentLinkTarget?): BookDocumentLinkTarget? {
    val fragment = when (target) {
        is BookDocumentLinkTarget.Anchor -> target.fragment
        is BookDocumentLinkTarget.Resource -> target.fragment
        else -> null
    }
    val pairedNote = when {
        id().startsWith("fnref-") -> fragment == "fn-${id().removePrefix("fnref-")}"
        id().startsWith("FNanchor_") -> fragment == "Footnote_${id().removePrefix("FNanchor_")}"
        else -> false
    }
    val explicitNote = attr("role").split(Regex("\\s+")).any { it.equals("doc-noteref", true) } ||
        attr("epub:type").split(Regex("\\s+")).any { it.equals("noteref", true) }
    if (!explicitNote && !pairedNote) return target
    return when (target) {
        is BookDocumentLinkTarget.Anchor -> BookDocumentLinkTarget.Reference(fragment = target.fragment)
        is BookDocumentLinkTarget.Resource -> target.fragment?.let {
            BookDocumentLinkTarget.Reference(target.resourceId, it)
        }
        is BookDocumentLinkTarget.Reference -> target
        else -> null
    }
}
