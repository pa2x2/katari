package app.katari.desktop.clipboard

import app.katari.core.platform.clipboard.ImageClipboard
import app.katari.presentation.kit.feedback.AppMessages
import dev.zacsweers.metro.Inject
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import tachiyomi.i18n.clipboard_copy_error
import tachiyomi.i18n.copied_to_clipboard_plain
import tachiyomi.i18n.localized.stringResource
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage

/** Images on the system clipboard through AWT, as [AwtTextClipboard] puts text there, confirmed in [messages]. */
@Inject
class AwtImageClipboard(private val messages: AppMessages) : ImageClipboard {
    override fun copy(image: BufferedImage) {
        try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageSelection(image), null)
            messages.show(stringResource(MR.strings.copied_to_clipboard_plain))
        } catch (e: IllegalStateException) {
            // Another application holds the clipboard.
            logcat(LogPriority.ERROR, e) { "Could not copy the image to the clipboard" }
            messages.show(stringResource(MR.strings.clipboard_copy_error))
        }
    }
}

private class ImageSelection(private val image: Image) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor != DataFlavor.imageFlavor) throw UnsupportedFlavorException(flavor)
        return image
    }
}
