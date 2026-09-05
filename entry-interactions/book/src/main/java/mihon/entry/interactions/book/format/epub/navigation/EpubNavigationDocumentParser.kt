package mihon.entry.interactions.book.format.epub.navigation

import mihon.book.api.BookNavigationItem
import mihon.entry.interactions.book.format.epub.EpubContract
import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.packageinfo.EpubManifestItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

internal class EpubNavigationDocumentParser(private val archive: EpubArchive) {
    fun parse(item: EpubManifestItem): List<BookNavigationItem> {
        val document = Jsoup.parse(archive.readText(item.resourceId, EpubContract.MAX_DOCUMENT_BYTES))
        val navigation = document.getAllElements().firstOrNull { element ->
            element.normalName() == "nav" &&
                listOf(element.attr("epub:type"), element.attr("type"))
                    .flatMap { it.split(Regex("\\s+")) }
                    .any { it == "toc" }
        } ?: document.getAllElements().firstOrNull { it.normalName() == "nav" } ?: return emptyList()
        val list = navigation.children().firstOrNull { it.normalName() in LIST_ELEMENTS } ?: return emptyList()
        return list.children().filter { it.normalName() == "li" }.flatMap { child ->
            parseNavigationListItem(child, item.resourceId)
        }
    }

    private fun parseNavigationListItem(element: Element, baseResource: String): List<BookNavigationItem> {
        val link = element.children().firstOrNull { it.normalName() == "a" }
        val target = link?.attr("href")?.let { href -> epubNavigationLocator(baseResource, href) }
        val nested = element.children().firstOrNull { it.normalName() in LIST_ELEMENTS }
            ?.children()
            ?.filter { it.normalName() == "li" }
            ?.flatMap { parseNavigationListItem(it, baseResource) }
            .orEmpty()
        if (target == null) return nested
        return listOf(
            BookNavigationItem(
                title = link.text().trim().takeIf(String::isNotEmpty),
                target = target,
                children = nested,
            ),
        )
    }

    private companion object {
        val LIST_ELEMENTS = setOf("ol", "ul")
    }
}
