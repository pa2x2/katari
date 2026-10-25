package mihon.video.engine.dash

import org.jsoup.nodes.Element

/*
 * Element access by local name: manifests are matched by their elements' names whatever namespace prefix a packager
 * gave them, and jsoup's XML parser keeps names as written.
 */

internal val Element.localName: String
    get() = tagName().substringAfter(':')

/** The element's namespace prefix with its colon, for an element added beside it; empty when it has none. */
internal val Element.namespacePrefix: String
    get() = tagName().substringBefore(':', "").let { if (it.isEmpty()) "" else "$it:" }

internal fun Element.childrenNamed(name: String): List<Element> = children().filter { it.localName == name }

internal fun Element.firstChildNamed(name: String): Element? = children().firstOrNull { it.localName == name }

internal fun Element.attrOrNull(name: String): String? = if (hasAttr(name)) attr(name).trim() else null

internal fun Element.longAttr(name: String): Long? = attrOrNull(name)?.toLongOrNull()

internal val ADDRESSING_ELEMENTS = listOf("SegmentTemplate", "SegmentList", "SegmentBase")
