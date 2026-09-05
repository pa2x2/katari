package mihon.entry.interactions.book.format.epub.navigation

import mihon.book.api.BookNavigationItem
import mihon.entry.interactions.book.format.epub.EpubContract
import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.packageinfo.EpubManifestItem
import mihon.entry.interactions.book.format.epub.xml.EPUB_NCX_NAMESPACE
import mihon.entry.interactions.book.format.epub.xml.hasEpubXmlName
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

internal class EpubLegacyNavigationParser(private val archive: EpubArchive) {
    fun parse(item: EpubManifestItem): List<BookNavigationItem> {
        val document = Jsoup.parse(
            archive.readText(item.resourceId, EpubContract.MAX_XML_BYTES),
            "",
            Parser.xmlParser(),
        )
        val map =
            document.getAllElements().firstOrNull { it.hasEpubXmlName("navmap", EPUB_NCX_NAMESPACE) }
                ?: return emptyList()
        return map.children().filter { it.hasEpubXmlName("navpoint", EPUB_NCX_NAMESPACE) }.mapNotNull { point ->
            parseLegacyPoint(point, item.resourceId)
        }
    }

    private fun parseLegacyPoint(element: Element, baseResource: String): BookNavigationItem? {
        val source =
            element.children().firstOrNull { it.hasEpubXmlName("content", EPUB_NCX_NAMESPACE) }?.attr("src")
                ?: return null
        val target = epubNavigationLocator(baseResource, source) ?: return null
        val title = element.children().firstOrNull { it.hasEpubXmlName("navlabel", EPUB_NCX_NAMESPACE) }
            ?.text()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        return BookNavigationItem(
            title = title,
            target = target,
            children = element.children().filter {
                it.hasEpubXmlName("navpoint", EPUB_NCX_NAMESPACE)
            }.mapNotNull { point ->
                parseLegacyPoint(point, baseResource)
            },
        )
    }
}
