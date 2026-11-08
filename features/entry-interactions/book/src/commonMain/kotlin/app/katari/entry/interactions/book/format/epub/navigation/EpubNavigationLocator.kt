package app.katari.entry.interactions.book.format.epub.navigation

import app.katari.book.api.BookLocator
import app.katari.entry.interactions.book.format.epub.archive.EpubArchiveReference
import app.katari.entry.interactions.book.format.epub.archive.resolveArchiveReference

internal fun epubNavigationLocator(baseResource: String, href: String): BookLocator? {
    val target = resolveArchiveReference(baseResource, href) as? EpubArchiveReference.Internal ?: return null
    return BookLocator(
        resourceId = target.path,
        progression = 0.0,
        fragments = listOfNotNull(target.fragment),
    )
}
