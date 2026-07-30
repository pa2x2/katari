package mihon.entry.interactions.book.prose

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.EntryChapter

@RunWith(RobolectricTestRunner::class)
internal abstract class HtmlProseDocumentFixture {
    protected fun prepare(html: String) = prepareHtmlBookDocument(
        resourceId = "chapter-1",
        revision = "r1",
        bodyHtml = html,
    )

    protected fun chapter() = EntryChapter.create().copy(id = 1L, name = "Chapter 1")
}
