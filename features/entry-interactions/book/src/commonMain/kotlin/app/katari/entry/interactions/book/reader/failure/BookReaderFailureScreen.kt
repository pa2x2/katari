package app.katari.entry.interactions.book.reader.failure

import androidx.compose.runtime.Composable
import app.katari.book.api.BookFailureReason
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Book
import app.katari.icons.materialsymbols.rounded.Error
import app.katari.icons.materialsymbols.rounded.Public
import app.katari.icons.materialsymbols.rounded.Refresh
import app.katari.presentation.kit.feedback.ProblemAction
import app.katari.presentation.kit.feedback.ProblemScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.i18n.stringResource as mokoStringResource

/**
 * Says why a book reader couldn't open a chapter. [sourceName] is set when [message] is the source's own, such as a
 * chapter it keeps locked; [onOpenWebView] leads to the source's page for it, and is the main way on when available.
 */
@Composable
internal fun BookReaderFailureScreen(
    reason: BookFailureReason,
    message: String,
    heading: BookReaderChapterHeading?,
    onClose: () -> Unit,
    onRetry: (() -> Unit)?,
    sourceName: String? = null,
    onOpenWebView: (() -> Unit)? = null,
) {
    val bookCannotOpen = reason == BookFailureReason.PROCESSOR_UNAVAILABLE ||
        reason == BookFailureReason.FORMAT_UNSUPPORTED
    val actions = buildList {
        onOpenWebView?.let {
            add(ProblemAction(mokoStringResource(MR.strings.action_web_view), it, MaterialSymbols.Rounded.Public))
        }
        onRetry?.let {
            add(ProblemAction(mokoStringResource(MR.strings.action_retry), it, MaterialSymbols.Rounded.Refresh))
        }
        if (isEmpty()) add(ProblemAction(stringResource(MR.strings.book_reader_back_to_book), onClose))
    }
    ProblemScreen(
        icon = if (bookCannotOpen) MaterialSymbols.Rounded.Book else MaterialSymbols.Rounded.Error,
        heading = if (bookCannotOpen) {
            stringResource(MR.strings.book_reader_failure_book)
        } else {
            mokoStringResource(MR.strings.transition_chapter_failed)
        },
        message = message,
        navigateUp = onClose,
        actions = actions,
        title = heading?.bookTitle,
        subtitle = heading?.chapterName,
        attribution = sourceName?.let { stringResource(MR.strings.book_reader_failure_source_message, it) },
    )
}
