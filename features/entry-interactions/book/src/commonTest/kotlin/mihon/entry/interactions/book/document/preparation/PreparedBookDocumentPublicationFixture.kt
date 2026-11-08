package mihon.entry.interactions.book.document.preparation

import app.katari.book.api.BookPublication
import app.katari.book.api.BookReadingDirection
import app.katari.book.api.BookResource
import app.katari.book.api.document.BookDocumentPublicationModel
import io.mockk.mockk
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer

internal fun preparedDocumentPublication(vararg content: Pair<String, String>): PreparedBookDocumentPublication {
    val documents = content.map { (id, html) ->
        HtmlProseDocumentParser().parse(id, "revision", HtmlProseSanitizer.sanitize(html.encodeToByteArray()))
    }
    return PreparedBookDocumentPublication(
        publication = BookPublication(
            id = "publication",
            revision = "revision",
            title = null,
            languages = emptyList(),
            readingDirection = BookReadingDirection.LEFT_TO_RIGHT,
            navigation = emptyList(),
            readingOrder = documents.map { BookResource(it.resourceId, "text/html", null) },
        ),
        model = BookDocumentPublicationModel(documents),
        resourceLoader = mockk(),
    )
}
