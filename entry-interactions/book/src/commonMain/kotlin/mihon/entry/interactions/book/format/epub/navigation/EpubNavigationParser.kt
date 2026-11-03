package mihon.entry.interactions.book.format.epub.navigation

import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.entry.interactions.book.format.epub.archive.EpubArchive
import mihon.entry.interactions.book.format.epub.packageinfo.EpubPackage

internal class EpubNavigationParser(
    private val archive: EpubArchive,
) {
    fun parse(packageInfo: EpubPackage): List<BookNavigationItem> {
        val authored =
            packageInfo.navigationResource?.takeUnless {
                it.isRemote
            }?.let(EpubNavigationDocumentParser(archive)::parse)
                ?.takeIf(List<BookNavigationItem>::isNotEmpty)
                ?: packageInfo.legacyNavigationResource?.takeUnless {
                    it.isRemote
                }?.let(EpubLegacyNavigationParser(archive)::parse).orEmpty()
        val targeted = authored.flatMapTo(mutableSetOf()) { item -> item.resourceIds() }
        val readingIndex = packageInfo.readingOrder.withIndex().associate { (index, item) -> item.resourceId to index }
        val fallbacks = ArrayDeque(
            packageInfo.readingOrder.filterNot { it.resourceId in targeted }.map { item ->
                BookNavigationItem(
                    title = null,
                    target = BookLocator(resourceId = item.resourceId, progression = 0.0),
                )
            },
        )
        // Documents the authored navigation leaves out, typically a cover page, keep their
        // reading-order place instead of trailing the whole table of contents.
        return buildList {
            authored.forEach { item ->
                readingIndex[item.target.resourceId]?.let { itemIndex ->
                    while (
                        fallbacks.isNotEmpty() &&
                        readingIndex.getValue(fallbacks.first().target.resourceId) < itemIndex
                    ) {
                        add(fallbacks.removeFirst())
                    }
                }
                add(item)
            }
            addAll(fallbacks)
        }
    }

    private fun BookNavigationItem.resourceIds(): List<String> =
        listOf(target.resourceId) + children.flatMap { it.resourceIds() }
}
