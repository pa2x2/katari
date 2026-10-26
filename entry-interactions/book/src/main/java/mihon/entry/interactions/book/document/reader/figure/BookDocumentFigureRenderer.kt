package mihon.entry.interactions.book.document.reader.figure

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.BookDocumentRichTextRenderer
import mihon.entry.interactions.book.document.reader.BookDocumentSelectableText
import mihon.entry.interactions.book.document.reader.LocalBookDocumentChapterSelection
import mihon.entry.interactions.book.document.resource.BookDocumentImageHeaders
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.ProseImageHeader
import mihon.entry.interactions.book.document.resource.decodeValidatedProseImage
import mihon.entry.interactions.book.document.resource.readProseImageHeader
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * Bounded, composition-scoped figure resource renderer. A lazy list forgets a row's bitmap once it scrolls away,
 * so the figure takes its height from the image header whenever that is known: a row that grew only when its
 * bitmap arrived would push the rows below it down while it sits at the top of the viewport.
 */
@Composable
internal fun BookDocumentFigureRenderer(
    content: BookDocumentBlockContent.Figure,
    block: BookDocumentBlock,
    selectionIdentity: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
) {
    val selection = LocalBookDocumentChapterSelection.current
    val resourceId = content.image.resourceId
    val resourceGeneration = resourceLoader?.generation?.collectAsState()?.value ?: 0
    var retryGeneration by remember(resourceId) { mutableIntStateOf(0) }
    var header by remember(resourceLoader, resourceId) {
        mutableStateOf(resourceLoader?.let { BookDocumentImageHeaders.cached(it, resourceId) })
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val targetWidth = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val bitmapResult by produceState<Result<Bitmap>?>(
            initialValue = null,
            resourceId,
            resourceLoader,
            targetWidth,
            retryGeneration,
            resourceGeneration,
        ) {
            value = if (resourceLoader == null) {
                Result.failure(IllegalStateException("Image resource unavailable"))
            } else {
                runCatching {
                    val resource = resourceLoader.load(
                        resourceId = resourceId,
                        acceptedMediaTypes = PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
                        maxBytes = PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
                    ).getOrThrow()
                    val (decodedHeader, bitmap) = withContext(Dispatchers.Default) {
                        readProseImageHeader(resource.bytes, resource.mediaType) to decodeValidatedProseImage(
                            bytes = resource.bytes,
                            mediaType = resource.mediaType,
                            targetWidthPx = targetWidth,
                            targetHeightPx = targetWidth * 2,
                        )
                    }
                    BookDocumentImageHeaders.record(resourceLoader, resourceId, decodedHeader)
                    header = decodedHeader
                    bitmap
                }
            }
        }
        val bitmap = bitmapResult?.getOrNull()
        val alternativeText = content.image.alternativeText?.text
        val captionText = content.caption?.text
        DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
        val imageModifier = header
            ?.let { with(density) { Modifier.fillMaxWidth().height(it.figureHeightPx(targetWidth).toDp()) } }
            ?: Modifier.fillMaxWidth().sizeIn(maxHeight = maxWidth * 2)
        when {
            bitmap != null -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = when {
                    content.image.decorative -> null
                    alternativeText != null -> alternativeText
                    captionText != null -> captionText
                    else -> stringResource(R.string.book_document_image_description_unavailable)
                },
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
            )
            bitmapResult == null -> Box(modifier = imageModifier) {
                DisableSelection {
                    Text(stringResource(R.string.book_document_image_loading), role = TextRole.ItemTitle)
                }
            }
            else -> {
                val fallback = content.image.alternativeText
                val fallbackText = fallback?.text ?: stringResource(R.string.book_document_image_unavailable)
                BookDocumentSelectableText(
                    text = fallbackText,
                    links = fallback?.links.orEmpty(),
                    inlineStyles = fallback?.inlineStyles.orEmpty(),
                    identity = "$selectionIdentity:image-fallback",
                    block = block,
                    separatorAfter = if (content.caption == null) "\n\n" else "\n",
                    onAnchorClick = onAnchorClick,
                    onExternalLinkClick = onExternalLinkClick,
                    standIn = true,
                    modifier = Modifier.clickable {
                        selection?.handleReaderTap { retryGeneration++ } ?: run { retryGeneration++ }
                    },
                )
            }
        }
    }
    content.caption?.let {
        BookDocumentRichTextRenderer(
            value = it,
            identity = "$selectionIdentity:caption",
            block = block,
            onAnchorClick = onAnchorClick,
            onExternalLinkClick = onExternalLinkClick,
            separatorAfter = "\n\n",
            modifier = Modifier.padding(top = KatariTheme.spacing.s),
        )
    }
}

/**
 * The height the decoded bitmap takes in a figure [widthPx] wide: images are shown at their decoded pixel size,
 * which decoding already keeps within the width and twice the width.
 */
private fun ProseImageHeader.figureHeightPx(widthPx: Int): Int = decodedSize(widthPx, widthPx * 2).height
