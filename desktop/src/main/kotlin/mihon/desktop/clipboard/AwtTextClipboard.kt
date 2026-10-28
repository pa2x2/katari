package mihon.desktop.clipboard

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.lang.truncateCenter
import logcat.LogPriority
import mihon.core.platform.clipboard.TextClipboard
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.clipboard_copy_error
import tachiyomi.i18n.copied_to_clipboard
import tachiyomi.i18n.localized.stringResource
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * The system clipboard through AWT, which reaches it on X11 and, through XWayland, on Wayland. Desktop clipboards take
 * no label and confirm nothing themselves, so the copy is confirmed in [messages], as Android does before 13.
 */
@Inject
class AwtTextClipboard(private val messages: AppMessages) : TextClipboard {
    override fun copy(label: String, text: String) {
        if (text.isBlank()) return
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            messages.show(stringResource(MR.strings.copied_to_clipboard, text.truncateCenter(50)))
        } catch (e: IllegalStateException) {
            // Another application holds the clipboard.
            logcat(LogPriority.ERROR, e) { "Could not copy to the clipboard" }
            messages.show(stringResource(MR.strings.clipboard_copy_error))
        }
    }
}
