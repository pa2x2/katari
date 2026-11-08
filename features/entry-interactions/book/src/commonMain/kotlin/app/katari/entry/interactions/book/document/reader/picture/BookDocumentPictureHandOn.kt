package app.katari.entry.interactions.book.document.reader.picture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.currentPlatformContext
import app.katari.entry.interactions.reader.image.export.ReaderImageExport
import app.katari.presentation.kit.feedback.ScreenMessages
import app.katari.presentation.kit.feedback.rememberScreenMessages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Hands a book's picture out of the app, where [export] offers it: the book's own file of the picture named
 * [resourceId], which [bytes] reads, rather than the picture decoded for the screen. Saving says when it is done, and
 * saving or sharing when it fails; copying, as quiet as the clipboard, only logs a failure.
 */
@Stable
internal class BookDocumentPictureHandOn(
    private val context: PlatformContext,
    val export: ReaderImageExport,
    private val scope: CoroutineScope,
    private val messages: ScreenMessages,
    private val savedMessage: String,
    private val failedMessage: String,
) {
    fun save(resourceId: String, bytes: suspend () -> ByteArray) = handOn(failedMessage) {
        val file = bytes()
        export.save(context, { file.inputStream() }, resourceId.pictureName(), folder = null)
        messages.show(savedMessage)
    }

    fun share(resourceId: String, bytes: suspend () -> ByteArray) = handOn(failedMessage) {
        val file = bytes()
        export.share(context, { file.inputStream() }, resourceId.pictureName(), message = null)
    }

    fun copy(resourceId: String, bytes: suspend () -> ByteArray) = handOn(failedMessage = null) {
        val file = bytes()
        export.copy(context, { file.inputStream() }, resourceId.pictureName())
    }

    private fun handOn(failedMessage: String?, block: suspend () -> Unit) {
        scope.launch {
            runCatching { block() }.onFailure { error ->
                logcat(LogPriority.ERROR, error) { "Failed to hand on a BOOK picture" }
                failedMessage?.let { messages.show(it) }
            }
        }
    }
}

@Composable
internal fun rememberBookDocumentPictureHandOn(): BookDocumentPictureHandOn {
    val context = currentPlatformContext()
    val export = LocalBookDocumentPictureExport.current
    val scope = rememberCoroutineScope()
    val messages = rememberScreenMessages()
    val savedMessage = stringResource(MR.strings.picture_saved)
    val failedMessage = stringResource(MR.strings.error_saving_picture)
    return remember(context, export, scope, messages, savedMessage, failedMessage) {
        BookDocumentPictureHandOn(context, export, scope, messages, savedMessage, failedMessage)
    }
}

/** The picture's file name in the book, without its folder or extension, which the export adds for its type. */
private fun String.pictureName(): String =
    substringAfterLast('/').substringBeforeLast('.').ifBlank { DEFAULT_PICTURE_NAME }

private const val DEFAULT_PICTURE_NAME = "picture"
