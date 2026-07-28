package mihon.entry.interactions.book.epub

import android.graphics.RectF
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.ui.geometry.Offset
import mihon.translation.ui.session.TranslationSelectionAnchor

internal class ReadiumSelectionChangeBridge(
    private val onSelectionChanged: () -> Unit,
) {
    /** The WebView only sends a signal; selected content is resolved through Readium's Kotlin API. */
    @JavascriptInterface
    fun selectionChanged() {
        onSelectionChanged()
    }
}

internal fun RectF.toReaderRootAnchor(
    navigatorViewPositionInWindow: Offset,
    readerRootPositionInWindow: Offset,
    readiumContentTopInset: Float = 0f,
): TranslationSelectionAnchor {
    val x = navigatorViewPositionInWindow.x - readerRootPositionInWindow.x
    val y = navigatorViewPositionInWindow.y - readerRootPositionInWindow.y
    return TranslationSelectionAnchor(
        left = left + x,
        top = top + y,
        right = right + x,
        // Readium 3.3's adjustedToViewport() adds the page's top inset only to RectF.top.
        // Recover the missing bottom offset from the selected WebView's actual position.
        bottom = bottom + readiumContentTopInset + y,
    )
}

/**
 * Returns the selected Readium WebView's vertical origin in the navigator's coordinate space.
 *
 * Selection gives focus to the owning WebView. Resolving the offset from that focused descendant avoids depending on
 * Readium's private page-fragment hierarchy and remains correct when its viewport padding changes.
 */
internal fun View.readiumSelectionContentTopInset(): Float {
    val focusedView = findFocus() ?: return 0f
    val selectedWebView = generateSequence(focusedView) { view ->
        view.parent as? View
    }.filterIsInstance<WebView>().firstOrNull() ?: return 0f
    val navigatorPosition = IntArray(2).also(::getLocationInWindow)
    val contentPosition = IntArray(2).also(selectedWebView::getLocationInWindow)
    return (contentPosition[1] - navigatorPosition[1]).coerceAtLeast(0).toFloat()
}

internal const val READIUM_SELECTION_JAVASCRIPT_INTERFACE = "KatariSelection"

internal val INSTALL_READIUM_SELECTION_LISTENER_SCRIPT = """
    (function() {
        if (window.__katariSelectionTranslationListenerInstalled) return true;
        window.__katariSelectionTranslationListenerInstalled = true;
        document.addEventListener("selectionchange", function() {
            KatariSelection.selectionChanged();
        });
        return true;
    })();
""".trimIndent()
