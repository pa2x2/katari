package mihon.entry.interactions.book.document.reader

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentTextLinkDispatchTest : BookDocumentTextViewFixture() {
    @Test
    fun `same-document URL span dispatches its anchor instead of opening a URL`() {
        val source = SpannableString("See note\n\n").apply {
            setSpan(URLSpan("#note"), 0, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        var clickedAnchor: String? = null
        val linked = source
            .withoutTerminalLayoutLine()
            .withDocumentAnchorClicks { anchor, _ -> clickedAnchor = anchor }
        val span = linked.getSpans(0, linked.length, ClickableSpan::class.java).single()

        span.onClick(BookDocumentTextView(RuntimeEnvironment.getApplication()))

        assertEquals("note", clickedAnchor)
        assertEquals("See note", linked.toString())
        assertTrue(linked.getSpans(0, linked.length, URLSpan::class.java).isEmpty())
    }

    @Test
    fun `safe external URL span dispatches an explicit external link action`() {
        val source = SpannableString("Open source").apply {
            setSpan(URLSpan("https://example.com/chapter"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        var openedUrl: String? = null
        val linked = source.withDocumentLinkClicks(
            onAnchorClick = { _, _ -> error("External link must not dispatch an anchor") },
            onExternalLinkClick = { openedUrl = it },
        )
        val span = linked.getSpans(0, linked.length, ClickableSpan::class.java).single()

        span.onClick(BookDocumentTextView(RuntimeEnvironment.getApplication()))

        assertEquals("https://example.com/chapter", openedUrl)
        assertTrue(linked.getSpans(0, linked.length, URLSpan::class.java).isEmpty())
    }

    @Test
    fun `mixed-case external scheme dispatches explicitly and unhandled spans are removed`() {
        val source = SpannableString("Uppercase and unsafe").apply {
            setSpan(URLSpan("HTTPS://example.com/chapter"), 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(URLSpan("mailto:test@example.com"), 14, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        var openedUrl: String? = null
        val linked = source.withDocumentLinkClicks(
            onAnchorClick = { _, _ -> error("External link must not dispatch an anchor") },
            onExternalLinkClick = { openedUrl = it },
        )

        val clickable = linked.getSpans(0, linked.length, ClickableSpan::class.java).single()
        clickable.onClick(BookDocumentTextView(RuntimeEnvironment.getApplication()))

        assertEquals("HTTPS://example.com/chapter", openedUrl)
        assertTrue(linked.getSpans(0, linked.length, URLSpan::class.java).isEmpty())
        assertEquals(0, linked.getSpans(14, linked.length, ClickableSpan::class.java).size)
    }

    @Test
    fun `retained link spans dispatch through the latest view callbacks`() {
        val source = SpannableString("See note").apply {
            setSpan(URLSpan("#note"), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val view = BookDocumentTextView(RuntimeEnvironment.getApplication())
        val linked = source.withDocumentLinkClicks(
            onAnchorClick = { _, _ -> error("The view callback must own retained link dispatch") },
            onExternalLinkClick = { error("The anchor must not dispatch externally") },
        )
        var dispatch = "initial"
        view.onDocumentAnchorClick = { anchor, _ -> dispatch = "first:$anchor" }
        view.updateDocumentText("chapter:block", linked)
        val retainedSpan = (view.text as Spanned)
            .getSpans(0, view.text.length, ClickableSpan::class.java)
            .single()
        retainedSpan.onClick(view)
        view.onDocumentAnchorClick = { anchor, _ -> dispatch = "latest:$anchor" }
        view.updateDocumentText(
            "chapter:block",
            source.withDocumentAnchorClicks { _, _ -> error("Equivalent text must not replace retained spans") },
        )

        retainedSpan.onClick(view)

        assertEquals("latest:note", dispatch)
    }
}
