package mihon.entry.interactions.book.document.reader.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.text.TextMeasurer
import mihon.book.api.document.BookDocumentFontFamily
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.book.document.reader.LocalBookDocumentResourceLoader
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTypography
import mihon.entry.interactions.book.document.reader.rememberBookDocumentFontResources
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentAuthoredStyling
import tachiyomi.domain.entry.model.EntryChapter

@Composable
internal fun rememberBookDocumentPageTextMeasurer(
    items: List<BookDocumentViewerItem<EntryChapter>>,
): BookDocumentPageTextMeasurer {
    val sections = remember(items) {
        items.mapNotNull { (it as? BookDocumentViewerItem.Block)?.section }.distinctBy { it.key }
    }
    val sectionFonts = buildMap {
        sections.forEach { section ->
            key(section.key) {
                CompositionLocalProvider(LocalBookDocumentResourceLoader provides section.resourceLoader) {
                    val resources = remember(section) {
                        section.document.blocks.flatMap { block ->
                            listOf(block.style.fontFamily) + block.inlineStyles.map { it.style.fontFamily }
                        }.filterIsInstance<BookDocumentFontFamily.Resource>().map { it.resourceId }.toSet()
                    }
                    val fonts = rememberBookDocumentFontResources(resources)
                    if (fonts.isNotEmpty()) put(section.key, fonts)
                }
            }
        }
    }
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val localeList = LocalLocaleList.current
    val resolver = LocalFontFamilyResolver.current
    val typography = LocalBookDocumentTypography.current
    val publisherStyles = LocalBookDocumentAuthoredStyling.current.enabled
    return remember(density, direction, localeList, resolver, typography, sectionFonts, publisherStyles) {
        BookDocumentPageTextMeasurer(
            TextMeasurer(resolver, localeList, density, direction, cacheSize = 0),
            density,
            typography,
            sectionFonts,
            publisherStyles,
        )
    }
}
