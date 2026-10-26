package eu.kanade.tachiyomi.source.lnreader.chapter

import eu.kanade.tachiyomi.source.entry.BookLinkedResources
import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import mihon.book.api.BookContentDescriptor

/**
 * The HTML `parseChapter` gives for the chapter at [path], as a prose chapter. Katari keeps the reader's place in a
 * novel by resource ID, so the HTML is named by the chapter's path. Pictures it names by path load from the plugin's
 * site with its image headers, as LNReader's reader loads them; the index's CSS file, when the plugin has one, styles
 * it as it does LNReader's reader.
 */
internal fun lnReaderChapterMedia(
    path: String,
    html: String,
    site: String,
    imageHeaders: Map<String, String>,
    customCss: String?,
): EntryMedia.Book {
    // What a plugin's fetchText gives when the request failed; LNReader shows a blank page
    if (html.isBlank()) throw IllegalStateException("The source sent an empty chapter")
    val chapter = liftInlineImages(html)
    val location = BookResourceLocation.InlineText(chapter.html, HTML)
    return EntryMedia.Book(
        descriptor = BookContentDescriptor(HTML, profile = PROSE_CHAPTER),
        catalog = BookResourceCatalog(
            resources = listOf(BookSourceResource(id = path, mediaType = HTML, location = location)) +
                chapter.images,
        ),
        initialResourceId = path,
        initialResourceLocation = location,
        linkedResources = BookLinkedResources(baseUrl = site.takeIf(String::isNotBlank), headers = imageHeaders),
        styleSheets = listOfNotNull(customCss?.takeIf { it.length <= EntryMedia.Book.MAX_STYLE_SHEET_LENGTH }),
    )
}

private const val HTML = "text/html"
private const val PROSE_CHAPTER = "prose-chapter"
