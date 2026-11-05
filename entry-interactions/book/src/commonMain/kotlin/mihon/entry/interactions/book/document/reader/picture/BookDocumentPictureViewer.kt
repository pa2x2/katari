package mihon.entry.interactions.book.document.reader.picture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.core.platform.currentPlatformContext
import mihon.entry.interactions.book.document.reader.LocalBookDocumentResourceDecoding
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.ProseImage
import mihon.entry.interactions.book.document.resource.decodeValidatedProseImage
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.reader.image.export.ReaderImageExport
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Save
import mihon.icons.materialsymbols.rounded.Share
import mihon.presentation.kit.dialog.PictureViewer
import mihon.presentation.kit.feedback.ScreenMessages
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.window.currentWindowLayout
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** How the open book's pictures leave the app; the reader's host provides it. */
internal val LocalBookDocumentPictureExport = staticCompositionLocalOf<ReaderImageExport> {
    error("Book document picture export is not provided")
}

/**
 * A book's picture full screen, decoded again at up to twice the screen's size so zooming shows its detail. Share and
 * Save hand on the book's own file rather than the decoded picture, each where the platform offers it.
 */
@Composable
internal fun BookDocumentPictureViewer(
    resourceId: String,
    resourceLoader: BookPublicationResourceLoader,
    contentDescription: String?,
    onDismissRequest: () -> Unit,
) {
    val context = currentPlatformContext()
    val export = LocalBookDocumentPictureExport.current
    val decoding = LocalBookDocumentResourceDecoding.current
    val windowSize = currentWindowLayout().size
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val messages = rememberScreenMessages()
    val savedMessage = stringResource(MR.strings.picture_saved)
    val saveFailedMessage = stringResource(MR.strings.error_saving_picture)
    val maxSidePx = with(density) {
        maxOf(windowSize.width, windowSize.height).roundToPx() * ZOOM_DETAIL
    }.coerceAtMost(MAX_DECODED_SIDE_PX)
    val loaded by produceState<Pair<BookPublicationResource, ProseImage>?>(null, resourceId, resourceLoader) {
        value = resourceLoader.load(
            resourceId = resourceId,
            acceptedMediaTypes = PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
            maxBytes = PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
        ).mapCatching { resource ->
            resource to withContext(Dispatchers.Default) {
                decodeValidatedProseImage(resource.bytes, resource.mediaType, maxSidePx, maxSidePx, decoding)
            }
        }.onFailure { error ->
            logcat(LogPriority.ERROR, error) { "Failed to open a BOOK picture full screen" }
        }.getOrNull()
    }
    // Read once: inside onDispose, the delegated state would already be the next value, so disposing the effect started
    // for no picture would release the one that just arrived.
    val current = loaded
    DisposableEffect(current) { onDispose { current?.second?.release() } }
    val (resource, image) = current ?: return
    val name = resourceId.pictureName()

    PictureViewer(
        picture = image.bitmap,
        contentDescription = contentDescription,
        onDismissRequest = onDismissRequest,
        actions = listOfNotNull(
            AppBar.Action(
                title = stringResource(MR.strings.action_share),
                icon = MaterialSymbols.Rounded.Share,
                onClick = {
                    scope.handOn(messages, saveFailedMessage) {
                        export.share(context, { resource.bytes.inputStream() }, name, message = null)
                    }
                },
            ).takeIf { export.offersSharing },
            AppBar.Action(
                title = stringResource(MR.strings.action_save),
                icon = MaterialSymbols.Rounded.Save,
                onClick = {
                    scope.handOn(messages, saveFailedMessage) {
                        export.save(context, { resource.bytes.inputStream() }, name, folder = null)
                        messages.show(savedMessage)
                    }
                },
            ).takeIf { export.offersSaving },
        ),
    )
}

private fun CoroutineScope.handOn(messages: ScreenMessages, failedMessage: String, block: suspend () -> Unit) {
    launch {
        runCatching { block() }.onFailure { error ->
            logcat(LogPriority.ERROR, error) { "Failed to hand on a BOOK picture" }
            messages.show(failedMessage)
        }
    }
}

/** The picture's file name in the book, without its folder or extension, which the export adds for its type. */
private fun String.pictureName(): String =
    substringAfterLast('/').substringBeforeLast('.').ifBlank { DEFAULT_PICTURE_NAME }

// The viewer decodes up to this many times the screen's longer side, so a zoomed picture stays sharp, but no larger
// than the texture size every GPU draws.
private const val ZOOM_DETAIL = 2
private const val MAX_DECODED_SIDE_PX = 4096
private const val DEFAULT_PICTURE_NAME = "picture"
