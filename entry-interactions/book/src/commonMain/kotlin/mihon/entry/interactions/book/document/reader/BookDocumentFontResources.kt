package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.entry.interactions.book.document.resource.BookDocumentFontCache
/** Loads authored families through the same protected resource boundary as figures. */
@Composable
internal fun rememberBookDocumentFonts(
    blockFamily: BookDocumentFontFamily?,
    inlineStyles: List<BookDocumentInlineStyleRange>,
): Map<String, FontFamily> {
    val resources = remember(blockFamily, inlineStyles) {
        (listOf(blockFamily) + inlineStyles.map { it.style.fontFamily })
            .filterIsInstance<BookDocumentFontFamily.Resource>().map { it.resourceId }.toSet()
    }
    return rememberBookDocumentFontResources(resources)
}

@Composable
internal fun rememberBookDocumentFontResources(resources: Set<String>): Map<String, FontFamily> {
    if (resources.isEmpty()) return emptyMap()
    val loader = LocalBookDocumentResourceLoader.current
    val generation = loader?.generation?.collectAsState()?.value ?: 0
    val decoding = LocalBookDocumentResourceDecoding.current
    val fonts by produceState<Map<String, FontFamily>>(emptyMap(), loader, resources, generation, decoding) {
        value = if (loader == null) {
            emptyMap()
        } else {
            resources.mapNotNull { id ->
                BookDocumentFontCache.load(loader, id, generation, decoding)?.let { id to it }
            }.toMap()
        }
    }
    return fonts
}

internal fun BookDocumentFontFamily?.toComposeFontFamily(fonts: Map<String, FontFamily>): FontFamily = when (this) {
    is BookDocumentFontFamily.Resource -> fonts[resourceId] ?: FontFamily.Default
    is BookDocumentFontFamily.Generic -> when (family) {
        BookDocumentFontFamily.GenericFamily.SERIF -> FontFamily.Serif
        BookDocumentFontFamily.GenericFamily.SANS_SERIF -> FontFamily.SansSerif
        BookDocumentFontFamily.GenericFamily.MONOSPACE -> FontFamily.Monospace
    }
    null -> FontFamily.Default
}
