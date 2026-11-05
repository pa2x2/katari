package mihon.entry.interactions.book.reader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import logcat.LogPriority
import mihon.core.metro.metroGraph
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.runtime.requireBook
import mihon.entry.interactions.runtime.EntryInteractionActivity
import mihon.entry.interactions.runtime.setEntryInteractionContent
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.launchEntryChildWebViewAction
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/** Android's host of opening a book ([BookReaderHostController]), which starts the chosen reader's activity. */
internal class BookReaderHostActivity : EntryInteractionActivity() {
    private lateinit var controller: BookReaderHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = stringResource(MR.strings.book_reader_host_title)
        controller = BookReaderHostController(
            request = BookReaderRequest(
                entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L),
                chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L),
            ),
            dependencies = metroGraph<BookReaderGraph>().bookReaderDependencies,
            context = this,
            scope = lifecycleScope,
            onOpened = { processor, request, sessionToken ->
                startActivity(processor.createReaderIntent(this, request, sessionToken))
                finish()
            },
        )
        setEntryInteractionContent {
            BookReaderHostContent(
                controller = controller,
                onClose = ::finish,
                onOpenWebView = { sourceChapter, webView ->
                    launchEntryChildWebViewAction(
                        EntryChildWebViewAction.OPEN_IN_WEB_VIEW,
                        webView,
                        sourceChapter.owner.displayTitle,
                    ).onFailure { error ->
                        logcat(LogPriority.ERROR, error) { "Failed to open the BOOK chapter in WebView" }
                    }
                },
            )
        }
        controller.start()
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val EXTRA_CHAPTER_ID = "chapter_id"

        fun newIntent(context: Context, entry: Entry, chapter: EntryChapter): Intent {
            entry.requireBook()
            return Intent(context, BookReaderHostActivity::class.java).apply {
                putExtra(EXTRA_ENTRY_ID, entry.id)
                putExtra(EXTRA_CHAPTER_ID, chapter.id)
            }
        }
    }
}
