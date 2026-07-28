package mihon.entry.interactions.book.epub

import android.graphics.RectF
import android.webkit.JavascriptInterface
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
    nativeContainerPositionInWindow: Offset,
    readerRootPositionInWindow: Offset,
): TranslationSelectionAnchor {
    val x = nativeContainerPositionInWindow.x - readerRootPositionInWindow.x
    val y = nativeContainerPositionInWindow.y - readerRootPositionInWindow.y
    return TranslationSelectionAnchor(
        left = left + x,
        top = top + y,
        right = right + x,
        bottom = bottom + y,
    )
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
