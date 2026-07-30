package mihon.entry.interactions.book.document.reader

import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import androidx.compose.ui.geometry.Offset
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentTextSelectionTest : BookDocumentTextViewFixture() {
    @Test
    fun `reader text remains selectable without translation observation`() {
        val view = laidOutTextView(SpannableString("Independent selection")) as BookDocumentTextView
        var emitted = false
        view.selectionInteraction = BookDocumentTextInteraction(
            observeSelections = false,
            rootPositionInWindow = Offset.Zero,
            onSelection = { emitted = true },
            isReaderTapBlocked = { false },
            onBlockedReaderTap = {},
            onNonLinkTap = { _, _ -> },
        )

        Selection.setSelection(view.text as Spannable, 0, 11)

        assertTrue(view.isTextSelectable)
        assertEquals("Independent", view.text.subSequence(view.selectionStart, view.selectionEnd).toString())
        assertFalse(emitted)
    }

    @Test
    fun `equivalent updates preserve selection only for the same document text identity`() {
        val view = BookDocumentTextView(RuntimeEnvironment.getApplication())
        view.updateDocumentText("chapter:block", SpannableString("Stable selected text"))
        Selection.setSelection(view.text as Spannable, 7, 15)

        view.updateDocumentText("chapter:block", SpannableString("Stable selected text"))

        assertEquals(7, view.selectionStart)
        assertEquals(15, view.selectionEnd)

        view.updateDocumentText("chapter:other-block", SpannableString("Stable selected text"))

        assertEquals(view.selectionStart, view.selectionEnd)
    }

    @Test
    fun `selectable text emits the exact substring and reader-root bounds`() {
        val view = laidOutTextView(SpannableString("  exact selection  ")) as BookDocumentTextView
        var emitted: BookDocumentTextSelection? = null
        view.selectionInteraction = BookDocumentTextInteraction(
            observeSelections = true,
            rootPositionInWindow = Offset.Zero,
            onSelection = { emitted = it },
            isReaderTapBlocked = { false },
            onBlockedReaderTap = {},
            onNonLinkTap = { _, _ -> },
        )
        view.setTextIsSelectable(true)

        android.text.Selection.setSelection(view.text as android.text.Spannable, 2, 17)

        val selection = emitted as BookDocumentTextSelection.Changed
        assertEquals("exact selection", selection.text)
        assertTrue(selection.boundsInReaderRoot.width() > 0f)
        assertTrue(selection.boundsInReaderRoot.height() > 0f)
    }

    @Test
    fun `selected text republishes stable identity with refreshed reader-root bounds`() {
        val view = laidOutTextView(SpannableString("Selection follows scrolling")) as BookDocumentTextView
        val emitted = mutableListOf<BookDocumentTextSelection>()
        view.selectionInteraction = interaction(
            rootPositionInWindow = Offset.Zero,
            onSelection = emitted::add,
        )
        Selection.setSelection(view.text as Spannable, 0, 9)
        val initial = emitted.last() as BookDocumentTextSelection.Changed

        view.selectionInteraction = interaction(
            rootPositionInWindow = Offset(0f, 24f),
            onSelection = emitted::add,
        )
        view.refreshOwnedSelectionAnchor()

        val refreshed = emitted.last() as BookDocumentTextSelection.Changed
        assertEquals(initial.identity, refreshed.identity)
        assertEquals(initial.text, refreshed.text)
        assertEquals(initial.boundsInReaderRoot.top - 24f, refreshed.boundsInReaderRoot.top)
        assertEquals(initial.boundsInReaderRoot.bottom - 24f, refreshed.boundsInReaderRoot.bottom)
    }
}
