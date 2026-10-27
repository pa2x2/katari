package mihon.desktop.clipboard

import dev.zacsweers.metro.Inject
import logcat.LogPriority
import mihon.core.platform.clipboard.TextClipboard
import tachiyomi.core.common.util.system.logcat
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * The system clipboard through AWT, which reaches it on X11 and, through XWayland, on Wayland. Desktop clipboards take
 * no label. Desktop can only show the user a message from inside a composition yet, so a failed copy is only
 * logged.
 */
@Inject
class AwtTextClipboard : TextClipboard {
    override fun copy(label: String, text: String) {
        if (text.isBlank()) return
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        } catch (e: IllegalStateException) {
            // Another application holds the clipboard.
            logcat(LogPriority.ERROR, e) { "Could not copy to the clipboard" }
        }
    }
}
