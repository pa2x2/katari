package mihon.entry.interactions.book.document.reader.picture

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.decodeValidatedProseImage
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.reader.image.ReaderImage
import mihon.entry.interactions.reader.image.ReaderImageLocation
import mihon.entry.interactions.reader.image.ReaderImageSaver
import mihon.entry.interactions.reader.image.toReaderShareIntent
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Save
import mihon.icons.materialsymbols.rounded.Share
import mihon.presentation.kit.dialog.PictureViewer
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.window.currentWindowLayout
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A book's picture full screen, decoded again at up to twice the screen's size so zooming shows its detail. Share and
 * Save hand on the book's own file rather than the decoded picture.
 */
@Composable
internal fun BookDocumentPictureViewer(
    resourceId: String,
    resourceLoader: BookPublicationResourceLoader,
    contentDescription: String?,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val windowSize = currentWindowLayout().size
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val messages = rememberScreenMessages()
    val saver = remember(context) { ReaderImageSaver(context.applicationContext) }
    val savedMessage = stringResource(MR.strings.picture_saved)
    val saveFailedMessage = stringResource(MR.strings.error_saving_picture)
    val maxSidePx = with(density) {
        maxOf(windowSize.width, windowSize.height).roundToPx() * ZOOM_DETAIL
    }.coerceAtMost(MAX_DECODED_SIDE_PX)
    val loaded by produceState<Pair<BookPublicationResource, Bitmap>?>(null, resourceId, resourceLoader) {
        value = resourceLoader.load(
            resourceId = resourceId,
            acceptedMediaTypes = PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
            maxBytes = PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
        ).mapCatching { resource ->
            resource to withContext(Dispatchers.Default) {
                decodeValidatedProseImage(resource.bytes, resource.mediaType, maxSidePx, maxSidePx)
            }
        }.onFailure { error ->
            logcat(LogPriority.ERROR, error) { "Failed to open a BOOK picture full screen" }
        }.getOrNull()
    }
    // Read once: inside onDispose, the delegated state would already be the next value, so disposing the effect started
    // for no picture would recycle the one that just arrived.
    val current = loaded
    DisposableEffect(current) { onDispose { current?.second?.recycle() } }
    val (resource, bitmap) = current ?: return

    fun handOn(location: ReaderImageLocation, onSaved: (Uri) -> Unit) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (location == ReaderImageLocation.Cache) location.directory(context).deleteRecursively()
                    saver.save(ReaderImage({ resource.bytes.inputStream() }, resourceId.pictureName(), location))
                }
            }
                .onSuccess(onSaved)
                .onFailure { error ->
                    logcat(LogPriority.ERROR, error) { "Failed to hand on a BOOK picture" }
                    messages.show(saveFailedMessage)
                }
        }
    }

    PictureViewer(
        picture = remember(bitmap) { bitmap.asImageBitmap() },
        contentDescription = contentDescription,
        onDismissRequest = onDismissRequest,
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_share),
                icon = MaterialSymbols.Rounded.Share,
                onClick = {
                    handOn(ReaderImageLocation.Cache) { uri -> context.startShare(uri) }
                },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.action_save),
                icon = MaterialSymbols.Rounded.Save,
                onClick = {
                    handOn(ReaderImageLocation.Pictures.create()) { messages.show(savedMessage) }
                },
            ),
        ),
    )
}

private fun Context.startShare(uri: Uri) = startActivity(uri.toReaderShareIntent(this))

/** The picture's file name in the book, without its folder or extension, which the saver adds for its type. */
private fun String.pictureName(): String =
    substringAfterLast('/').substringBeforeLast('.').ifBlank { DEFAULT_PICTURE_NAME }

// The viewer decodes up to this many times the screen's longer side, so a zoomed picture stays sharp, but no larger
// than the texture size every GPU draws.
private const val ZOOM_DETAIL = 2
private const val MAX_DECODED_SIDE_PX = 4096
private const val DEFAULT_PICTURE_NAME = "picture"
