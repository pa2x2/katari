package app.katari.entry.interactions.book.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import app.katari.entry.interactions.book.reader.failure.BookReaderChapterHeading
import app.katari.entry.interactions.book.reader.failure.BookReaderFailureScreen
import app.katari.entry.interactions.source.EntryChildWebViewResolution

/** What [controller] shows while a book opens: loading, the choice of reader, or why it couldn't open. */
@Composable
internal fun BookReaderHostContent(
    controller: BookReaderHostController,
    onClose: () -> Unit,
    onOpenWebView: (SourceChapter, EntryChildWebViewResolution.Available) -> Unit,
) {
    when (val state = controller.state) {
        null -> BookReaderLoadingScreen(controller.progress)
        is BookReaderHostState.Unavailable -> {
            val sourceChapter = state.sourceChapter
            val webView = remember(sourceChapter) { sourceChapter?.let(controller::resolveWebView) }
            val heading by produceState<BookReaderChapterHeading?>(null) { value = controller.loadHeading() }
            BookReaderFailureScreen(
                reason = state.failure.reason,
                message = state.failure.message,
                heading = heading,
                onClose = onClose,
                onRetry = controller::retry.takeIf { state.canRetry },
                sourceName = sourceChapter?.sourceName,
                onOpenWebView = if (sourceChapter != null && webView != null) {
                    { onOpenWebView(sourceChapter, webView) }
                } else {
                    null
                },
            )
        }
        is BookReaderHostState.ChoiceRequired -> BookProcessorChooserDialog(
            state = state,
            onChoose = controller::choose,
            onClose = onClose,
        )
        is BookReaderHostState.ReaderSelected -> Unit
    }
}
