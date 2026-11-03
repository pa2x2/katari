package mihon.entry.interactions.book.format.epub.xml

import org.jsoup.nodes.Element

/** Matches expanded XML names, independent of the author's chosen namespace prefix. */
internal fun Element.hasEpubXmlName(localName: String, namespace: String): Boolean {
    if (normalName().substringAfter(':') != localName.lowercase()) return false
    val prefix = tagName().substringBefore(':', "")
    val declaration = if (prefix.isEmpty()) "xmlns" else "xmlns:$prefix"
    val declaredNamespace = generateSequence(this) { it.parent() }
        .firstOrNull { it.hasAttr(declaration) }
        ?.attr(declaration)
    return declaredNamespace == namespace
}

internal const val EPUB_PACKAGE_NAMESPACE = "http://www.idpf.org/2007/opf"
internal const val EPUB_CONTAINER_NAMESPACE = "urn:oasis:names:tc:opendocument:xmlns:container"
internal const val EPUB_ENCRYPTION_NAMESPACE = "http://www.w3.org/2001/04/xmlenc#"
internal const val EPUB_METADATA_NAMESPACE = "http://purl.org/dc/elements/1.1/"
internal const val EPUB_NCX_NAMESPACE = "http://www.daisy.org/z3986/2005/ncx/"
