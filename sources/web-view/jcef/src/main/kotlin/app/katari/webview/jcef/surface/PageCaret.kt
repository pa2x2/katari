package app.katari.webview.jcef.surface

import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Rectangle
import javax.swing.SwingUtilities

/**
 * Where the page's text caret is, in CSS pixels from the page's top left, for the input method's windows to sit at: the
 * start of the text being composed while Chromium composes, and otherwise where [locate] last found the caret. Chromium
 * tells the caret's bounds only while it composes, and only where the input method composes in the page: one that
 * shows the composed text in a window of its own, as the JetBrains Runtime asks of X11 input methods by default,
 * composes nothing in the page and needs the caret before anything is composed.
 */
internal class PageCaret(private val scope: CoroutineScope, private val locate: suspend () -> Rect?) {
    // Set on Chromium's threads
    @Volatile
    private var composition: Rect? = null

    // The rest on the main thread
    private var located: Rect? = null
    private var locating: Job? = null
    private var moved = false

    /** The caret, as a line of the text's height; null while the page has no caret that is known. */
    val bounds: Rect?
        get() = composition ?: located

    /** Chromium's [characterBounds] of the text it composes, which is empty once it composes nothing. */
    fun onCompositionChanged(characterBounds: Array<Rectangle>) {
        composition = characterBounds.firstOrNull()?.let { first ->
            Rect(first.x.toFloat(), first.y.toFloat(), first.x.toFloat(), (first.y + first.height).toFloat())
        }
    }

    /** Finds the caret again, as the page's selection, focus or content may have moved it; on any thread. */
    fun relocate() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(::relocate)
            return
        }
        // One search at a time; a move during one searches again after it
        if (locating?.isActive == true) {
            moved = true
            return
        }
        locating = scope.launch {
            do {
                moved = false
                located = locate()
            } while (moved)
        }
    }

    companion object {
        /**
         * The caret's left, top and height in the main frame's CSS pixels as a JSON array, or null where no element
         * has focus. A text field's caret is measured from the text before it in the field's font, which leaves out
         * lines a text area wraps; other editable content's caret is its selection's end. The script changes nothing on
         * the page, so the page's own scripts see nothing of it.
         */
        val SCRIPT = """
            (() => {
              let doc = document, field = doc.activeElement, x = 0, y = 0;
              while (field && field.contentDocument && field.contentDocument.activeElement) {
                const frame = field.getBoundingClientRect();
                x += frame.left + field.clientLeft;
                y += frame.top + field.clientTop;
                doc = field.contentDocument;
                field = doc.activeElement;
              }
              if (!field || field === doc.body || field === doc.documentElement) return null;
              const box = field.getBoundingClientRect();
              let caret = null;
              if ((field.tagName === 'INPUT' || field.tagName === 'TEXTAREA') && field.selectionEnd != null) {
                const style = getComputedStyle(field);
                const lines = field.value.substring(0, field.selectionEnd).split('\n');
                const context = new OffscreenCanvas(1, 1).getContext('2d');
                context.font = style.font;
                const fontHeight = parseFloat(style.fontSize) * 1.2;
                const lineHeight = parseFloat(style.lineHeight) || fontHeight;
                const left = box.left + field.clientLeft + parseFloat(style.paddingLeft) - field.scrollLeft +
                  context.measureText(lines[lines.length - 1]).width;
                caret = field.tagName === 'INPUT'
                  ? [left, box.top + (box.height - fontHeight) / 2, fontHeight]
                  : [left, box.top + field.clientTop + parseFloat(style.paddingTop) - field.scrollTop +
                      (lines.length - 1) * lineHeight, lineHeight];
                caret[0] = Math.min(caret[0], box.right);
              } else {
                const selection = doc.getSelection();
                if (selection && selection.rangeCount) {
                  const range = selection.getRangeAt(0).cloneRange();
                  range.collapse(false);
                  const rect = range.getClientRects()[0];
                  if (rect) caret = [rect.left, rect.top, rect.height];
                }
              }
              if (!caret) caret = [box.left, box.top, box.height];
              return [caret[0] + x, caret[1] + y, caret[2]];
            })()
        """.trimIndent()

        /** The caret [SCRIPT] found, from its [json] value; null where it found none. */
        fun parse(json: String?): Rect? {
            // Not numbers where the page made the measures fail
            val values = runCatching {
                (Json.parseToJsonElement(json ?: return null) as? JsonArray)?.map { it.jsonPrimitive.double.toFloat() }
            }.getOrNull()?.takeIf { it.size == 3 } ?: return null
            val (left, top, height) = values
            return Rect(left, top, left, top + height)
        }
    }
}
