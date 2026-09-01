package mihon.entry.interactions.book.format.epub.navigation

import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.entry.interactions.book.format.epub.EpubContract
import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.archive.EpubArchiveReference
import mihon.entry.interactions.book.format.epub.archive.resolveArchiveReference
import mihon.entry.interactions.book.format.epub.packageinfo.EpubManifestItem
import mihon.entry.interactions.book.format.epub.packageinfo.EpubPackage
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

internal class EpubNavigationParser(
    private val archive: EpubArchive,
) {
    fun parse(packageInfo: EpubPackage): List<BookNavigationItem> {
        val authored = packageInfo.navigationResource?.let(::parseNavigationDocument)
            ?.takeIf(List<BookNavigationItem>::isNotEmpty)
            ?: packageInfo.legacyNavigationResource?.let(::parseLegacyNavigation).orEmpty()
        val targeted = authored.flatMapTo(mutableSetOf()) { item -> item.resourceIds() }
        val fallbacks = packageInfo.readingOrder.filterNot { it.resourceId in targeted }.map { item ->
            BookNavigationItem(
                title = null,
                target = BookLocator(resourceId = item.resourceId, progression = 0.0),
            )
        }
        return authored + fallbacks
    }

    private fun parseNavigationDocument(item: EpubManifestItem): List<BookNavigationItem> {
        val document = Jsoup.parse(archive.readText(item.resourceId, EpubContract.MAX_DOCUMENT_BYTES))
        val navigation = document.getAllElements().firstOrNull { element ->
            element.normalName() == "nav" &&
                listOf(element.attr("epub:type"), element.attr("type"))
                    .flatMap { it.split(Regex("\\s+")) }
                    .any { it == "toc" }
        } ?: document.getAllElements().firstOrNull { it.normalName() == "nav" } ?: return emptyList()
        val list = navigation.children().firstOrNull { it.normalName() in LIST_ELEMENTS } ?: return emptyList()
        return list.children().filter { it.normalName() == "li" }.mapNotNull { child ->
            parseNavigationListItem(child, item.resourceId)
        }
    }

    private fun parseNavigationListItem(element: Element, baseResource: String): BookNavigationItem? {
        val link = element.children().firstOrNull { it.normalName() == "a" }
        val target = link?.attr("href")?.let { href -> locator(baseResource, href) }
        val nested = element.children().firstOrNull { it.normalName() in LIST_ELEMENTS }
            ?.children()
            ?.filter { it.normalName() == "li" }
            ?.mapNotNull { parseNavigationListItem(it, baseResource) }
            .orEmpty()
        if (target == null) return nested.singleOrNull()
        return BookNavigationItem(
            title = link.text().trim().takeIf(String::isNotEmpty),
            target = target,
            children = nested,
        )
    }

    private fun parseLegacyNavigation(item: EpubManifestItem): List<BookNavigationItem> {
        val document = Jsoup.parse(
            archive.readText(item.resourceId, EpubContract.MAX_XML_BYTES),
            "",
            Parser.xmlParser(),
        )
        val map = document.getAllElements().firstOrNull { it.normalName() == "navmap" } ?: return emptyList()
        return map.children().filter { it.normalName() == "navpoint" }.mapNotNull { point ->
            parseLegacyPoint(point, item.resourceId)
        }
    }

    private fun parseLegacyPoint(element: Element, baseResource: String): BookNavigationItem? {
        val source = element.children().firstOrNull { it.normalName() == "content" }?.attr("src") ?: return null
        val target = locator(baseResource, source) ?: return null
        val title = element.children().firstOrNull { it.normalName() == "navlabel" }
            ?.text()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        return BookNavigationItem(
            title = title,
            target = target,
            children = element.children().filter { it.normalName() == "navpoint" }.mapNotNull { point ->
                parseLegacyPoint(point, baseResource)
            },
        )
    }

    private fun locator(baseResource: String, href: String): BookLocator? {
        val target = resolveArchiveReference(baseResource, href) as? EpubArchiveReference.Internal ?: return null
        return BookLocator(
            resourceId = target.path,
            progression = 0.0,
            fragments = listOfNotNull(target.fragment),
        )
    }

    private fun BookNavigationItem.resourceIds(): List<String> =
        listOf(target.resourceId) + children.flatMap { it.resourceIds() }

    private companion object {
        val LIST_ELEMENTS = setOf("ol", "ul")
    }
}
