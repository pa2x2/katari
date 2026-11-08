package app.katari.webview.jcef.surface

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextEditingScope
import androidx.compose.ui.text.input.TextEditorState
import org.cef.browser.CefBrowser
import org.cef.misc.CefRange

/**
 * The text an input method composes in the page, carried to Chromium's IME calls, which show it in the page's field
 * as Chromium shows it in a browser: underlined until the input method commits it, which types it, or cancels it,
 * which takes it out again. The input method sees the composed text as the whole of the text it edits, as the page's
 * own text stays in Chromium. Everything here runs on the main thread.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class PageComposition(private val browser: () -> CefBrowser?) {
    private var composing by mutableStateOf("")

    /** The composed text with the caret at its end, which the window's input method session reads as it edits. */
    val state: TextEditorState = object : TextEditorState, CharSequence {
        override val text: String
            get() = composing

        override val length: Int
            get() = composing.length

        override val selection: TextRange
            get() = TextRange(composing.length)

        override val composition: TextRange?
            get() = if (composing.isEmpty()) null else TextRange(0, composing.length)

        override fun get(index: Int): Char = composing[index]

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            composing.subSequence(startIndex, endIndex)

        override fun toString(): String = composing
    }

    /**
     * Applies one change of the input method's, as the window's session hands it over: text it commits, then the text
     * it composes next, if any. A change that commits nothing and composes nothing cancels what was being composed.
     */
    fun edit(block: TextEditingScope.() -> Unit) {
        val edit = Edit().apply(block)
        val browser = browser()
        if (edit.committed.isNotEmpty()) browser?.ImeCommitText(edit.committed, NO_RANGE, 0)
        val next = edit.composing
        when {
            // Chromium underlines the text as it does for an input method that styles nothing itself
            !next.isNullOrEmpty() ->
                browser?.ImeSetComposition(next, emptyList(), NO_RANGE, CefRange(next.length, next.length))
            edit.finished && composing.isNotEmpty() -> browser?.ImeFinishComposingText(false)
            edit.committed.isEmpty() && composing.isNotEmpty() -> browser?.ImeCancelComposing()
        }
        composing = next.orEmpty()
    }

    /**
     * Keeps what was being composed as typed, as when the page loses focus mid-word. The window's session then has the
     * input method end its composition and ignores the text it commits for it, which the page already has.
     */
    fun finish() {
        if (composing.isEmpty()) return
        browser()?.ImeFinishComposingText(false)
        composing = ""
    }

    private class Edit : TextEditingScope {
        var committed = ""
        var composing: String? = null
        var finished = false

        override fun commitText(text: CharSequence, newCursorPosition: Int) {
            committed += text
            composing = null
        }

        override fun setComposingText(text: CharSequence, newCursorPosition: Int) {
            composing = text.toString()
        }

        override fun finishComposingText() {
            finished = true
            composing = null
        }

        // The input method sees only the composed text, so it has nothing else to select or delete
        override fun deleteSurroundingTextInCodePoints(lengthBeforeCursor: Int, lengthAfterCursor: Int) = Unit

        override fun setSelection(start: Int, end: Int) = Unit

        override fun setComposingRegion(start: Int, end: Int) = Unit
    }

    private companion object {
        // Chromium's invalid range: the composition, or the caret where nothing is composed
        val NO_RANGE = CefRange(-1, -1)
    }
}
