package app.katari.entry.interactions.book.reader

import android.content.Context
import android.content.Intent
import app.katari.entry.interactions.book.document.reader.BookDocumentReaderActivity
import app.katari.entry.interactions.book.document.reader.BookDocumentReaderProcessor
import app.katari.entry.interactions.book.processor.BookReaderProcessor
import app.katari.entry.interactions.book.processor.BookReaderRequest

/**
 * The activity that shows a processor's reader for a prepared BOOK session. A processor with no Android reader fails
 * like a reader that couldn't be started.
 */
internal fun BookReaderProcessor.createReaderIntent(
    context: Context,
    request: BookReaderRequest,
    sessionToken: String,
): Intent = when (id) {
    BookDocumentReaderProcessor.PROCESSOR_ID -> BookDocumentReaderActivity.newIntent(context, request, sessionToken)
    else -> throw IllegalStateException("BOOK reader processor $id has no Android reader")
}
