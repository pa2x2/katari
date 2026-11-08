package mihon.entry.interactions.book.format.epub.navigation

import mihon.entry.interactions.book.format.epub.packageinfo.EpubManifestItem
import mihon.entry.interactions.book.format.epub.packageinfo.EpubPackage

internal fun navigationPackage(legacy: Boolean): EpubPackage {
    val chapter = EpubManifestItem("chapter", "chapter.xhtml", "application/xhtml+xml", emptySet())
    val navigation = if (legacy) {
        EpubManifestItem("nav", "toc.ncx", "application/x-dtbncx+xml", emptySet())
    } else {
        EpubManifestItem("nav", "nav.xhtml", "application/xhtml+xml", setOf("nav"))
    }
    return EpubPackage(
        packageResource = "package.opf",
        manifest = mapOf(chapter.id to chapter, navigation.id to navigation),
        documents = listOf(chapter),
        readingOrder = listOf(chapter),
        navigationResource = navigation.takeUnless { legacy },
        legacyNavigationResource = navigation.takeIf { legacy },
        uniqueIdentifier = null,
        resourceProtectionAlgorithms = emptyMap(),
        languages = emptyList(),
        rightToLeft = false,
    )
}
