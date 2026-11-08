package mihon.video.engine.dash

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import org.jsoup.parser.Parser

/**
 * The `MPD` element of the manifest [text]. jsoup's XML parser resolves no DTDs or external entities, so a manifest
 * can't make the reader open files or URLs.
 */
internal fun parseMpd(text: String): Element {
    val document = Jsoup.parse(text.removePrefix(BYTE_ORDER_MARK), "", Parser.xmlParser())
    return document.children().firstOrNull { it.localName == "MPD" }
        ?: throw UnsupportedDashManifestException("No MPD element")
}

/** The edited document as XML, its declaration, comments and whitespace as they were read. */
internal fun Document.toXml(): String {
    outputSettings()
        .syntax(Document.OutputSettings.Syntax.xml)
        .escapeMode(Entities.EscapeMode.xhtml)
        .prettyPrint(false)
    return outerHtml()
}

private const val BYTE_ORDER_MARK = "\uFEFF"
