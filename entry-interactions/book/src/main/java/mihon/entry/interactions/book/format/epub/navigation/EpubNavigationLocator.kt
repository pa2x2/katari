package mihon.entry.interactions.book.format.epub.navigation

import mihon.book.api.BookLocator
import mihon.entry.interactions.book.format.epub.archive.EpubArchiveReference
import mihon.entry.interactions.book.format.epub.archive.resolveArchiveReference

internal fun epubNavigationLocator(baseResource: String, href: String): BookLocator? {
    val target = resolveArchiveReference(baseResource, href) as? EpubArchiveReference.Internal ?: return null
    return BookLocator(
        resourceId = target.path,
        progression = 0.0,
        fragments = listOfNotNull(target.fragment),
    )
}
