package mihon.entry.interactions.book.document.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.decodeValidatedProseImage
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader

/** Bounded, composition-scoped figure resource renderer. */
@Composable
internal fun BookDocumentFigureRenderer(
    content: BookDocumentBlockContent.Figure,
    block: BookDocumentBlock,
    selectionIdentity: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
) {
    val selection = LocalBookDocumentChapterSelection.current
    var retryGeneration by remember(content.image.resourceId) { mutableIntStateOf(0) }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val targetWidth = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val bitmapResult by produceState<Result<Bitmap>?>(
            initialValue = null,
            content.image.resourceId,
            resourceLoader,
            targetWidth,
            retryGeneration,
        ) {
            value = if (resourceLoader == null) {
                Result.failure(IllegalStateException("Image resource unavailable"))
            } else {
                runCatching {
                    val resource = resourceLoader.load(
                        resourceId = content.image.resourceId,
                        acceptedMediaTypes = PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
                        maxBytes = PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
                    ).getOrThrow()
                    withContext(Dispatchers.Default) {
                        decodeValidatedProseImage(
                            bytes = resource.bytes,
                            mediaType = resource.mediaType,
                            targetWidthPx = targetWidth,
                            targetHeightPx = targetWidth * 2,
                        )
                    }
                }
            }
        }
        val bitmap = bitmapResult?.getOrNull()
        val alternativeText = content.image.alternativeText?.text
        val captionText = content.caption?.text
        DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxHeight = maxWidth * 2)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        selection?.handleReaderTap(onReaderTap) ?: onReaderTap()
                    },
            )
            bitmapResult == null -> DisableSelection {
                Text(stringResource(R.string.book_document_image_loading))
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
                    contentAlpha = 0.72f,
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
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
