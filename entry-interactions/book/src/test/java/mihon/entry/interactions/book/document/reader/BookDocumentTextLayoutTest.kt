package mihon.entry.interactions.book.document.reader

import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.widget.TextView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
internal class BookDocumentTextLayoutTest : BookDocumentTextViewFixture() {
    @Test
    fun `terminal line trimming preserves whole-document height across separate block views`() {
        val whole = measuredTextView(SpannableString("First paragraph\n\nSecond paragraph\n\nThird paragraph"))
        val blocks = listOf(
            measuredTextView(SpannableString("First paragraph\n\n"), trimTerminalLine = true),
            measuredTextView(SpannableString("Second paragraph\n\n"), trimTerminalLine = true),
            measuredTextView(SpannableString("Third paragraph")),
        )

        assertEquals(whole.measuredHeight, blocks.sumOf(TextView::getMeasuredHeight))
    }

    @Test
    fun `selecting a complete trimmed paragraph keeps its text visible`() {
        val view = measuredTextView(
            SpannableString("First line\nSecond line\n\n"),
            trimTerminalLine = true,
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val text = view.text as Spannable

        Selection.setSelection(text, 0, text.length)
        view.bringPointIntoView(view.selectionEnd)

        assertEquals("First line\nSecond line\n", text.toString())
        assertEquals(0, view.selectionStart)
        assertEquals(text.length, view.selectionEnd)
        assertEquals(
            view.height - view.compoundPaddingTop - view.compoundPaddingBottom,
            view.layout.height,
        )
        assertEquals(0, view.scrollY)
    }
}
