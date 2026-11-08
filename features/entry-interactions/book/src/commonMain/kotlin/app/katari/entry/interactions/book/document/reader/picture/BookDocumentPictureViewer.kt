package app.katari.entry.interactions.book.document.reader.picture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import app.katari.entry.interactions.book.document.reader.LocalBookDocumentResourceDecoding
import app.katari.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import app.katari.entry.interactions.book.document.resource.ProseImage
import app.katari.entry.interactions.book.document.resource.decodeValidatedProseImage
import app.katari.entry.interactions.book.preparation.BookPublicationResource
import app.katari.entry.interactions.book.preparation.BookPublicationResourceLoader
import app.katari.entry.interactions.reader.image.export.ReaderImageExport
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Save
import app.katari.icons.materialsymbols.rounded.Share
import app.katari.presentation.kit.dialog.PictureViewer
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.window.currentWindowLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
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
    val handOn = rememberBookDocumentPictureHandOn()
    val export = handOn.export
    val decoding = LocalBookDocumentResourceDecoding.current
    val windowSize = currentWindowLayout().size
    val density = LocalDensity.current
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

    PictureViewer(
        picture = image.bitmap,
        contentDescription = contentDescription,
        onDismissRequest = onDismissRequest,
        actions = listOfNotNull(
            AppBar.Action(
                title = stringResource(MR.strings.action_share),
                icon = MaterialSymbols.Rounded.Share,
                onClick = { handOn.share(resourceId) { resource.bytes } },
            ).takeIf { export.offersSharing },
            AppBar.Action(
                title = stringResource(MR.strings.action_save),
                icon = MaterialSymbols.Rounded.Save,
                onClick = { handOn.save(resourceId) { resource.bytes } },
            ).takeIf { export.offersSaving },
        ),
    )
}

// The viewer decodes up to this many times the screen's longer side, so a zoomed picture stays sharp, but no larger
// than the texture size every GPU draws.
private const val ZOOM_DETAIL = 2
private const val MAX_DECODED_SIDE_PX = 4096
