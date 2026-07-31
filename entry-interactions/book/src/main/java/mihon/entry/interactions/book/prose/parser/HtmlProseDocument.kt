package mihon.entry.interactions.book.prose

import android.text.Spanned
import mihon.book.api.document.BookDocument
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import tachiyomi.domain.entry.model.EntryChapter

internal typealias HtmlProseLoadedChapter = BookDocumentSection<EntryChapter>

internal data class HtmlProsePage(
    val chapter: EntryChapter,
    val index: Int,
    val total: Int,
    val text: Spanned,
    val progression: Float,
    val sourceStart: Int = 0,
    val sourceEndExclusive: Int = sourceStart + text.length,
    val structuredBlock: PreparedBookDocumentBlock? = null,
)

/**
 * Adapts HTML prose into the processor-neutral structured BOOK document.
 *
 * HTML and Jsoup stay on this producer side of the boundary; only the platform-neutral semantic model crosses into
 * `book-api`.
 */
internal fun prepareHtmlBookDocument(
    resourceId: String,
    revision: String?,
    bodyHtml: String,
): PreparedBookDocument = prepareStructuredHtmlBookDocument(
    resourceId = resourceId,
    revision = revision,
    body = Jsoup.parseBodyFragment(bodyHtml).body(),
)

internal fun prepareHtmlBookDocument(
    resourceId: String,
    revision: String?,
    body: Element,
): PreparedBookDocument = prepareStructuredHtmlBookDocument(resourceId, revision, body)

internal fun parseHtmlBookDocument(
    resourceId: String,
    revision: String?,
    body: Element,
): BookDocument = parseStructuredHtmlBookDocument(resourceId, revision, body)
