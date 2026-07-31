package mihon.entry.interactions.book.prose

import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.Layout
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.BookPublicationResourceLoader
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.decodeValidatedProseImage
import kotlin.math.roundToInt

@Composable
internal fun ProseFigure(
    semantic: BookDocumentBlockContent.Figure,
    resourceLoader: BookPublicationResourceLoader?,
    foreground: Color,
    background: Color,
    documentTextIdentityPrefix: String,
    readerTypeface: Typeface,
    readerTextSizeSp: Float,
    lineSpacingMultiplier: Float,
    onAnchorClick: (String, TextView) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    modifier: Modifier,
) {
    val imageWidth = semantic.image.width?.takeIf { it > 0 }
    val aspectRatio = imageWidth
        ?.toFloat()
        ?.div(semantic.image.height?.takeIf { it > 0 } ?: imageWidth)
        ?.coerceIn(MIN_IMAGE_ASPECT_RATIO, MAX_IMAGE_ASPECT_RATIO)
        ?: DEFAULT_IMAGE_ASPECT_RATIO
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
    ) {
        val captionTypefaces by rememberInlineProseTypefaces(
            loader = resourceLoader,
            styles = semantic.caption?.inlineStyles.orEmpty(),
        )
        val density = LocalDensity.current
        val targetWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val targetHeightPx = (targetWidthPx / aspectRatio).roundToInt().coerceAtLeast(1)
        val image by produceState<LoadedProseImage?>(
            initialValue = null,
            semantic.image.resourceId,
            resourceLoader,
            targetWidthPx,
            targetHeightPx,
        ) {
            value = resourceLoader?.loadProseImage(
                resourceId = semantic.image.resourceId,
                targetWidthPx = targetWidthPx,
                targetHeightPx = targetHeightPx,
            )?.getOrNull()
        }
        val ownedBitmap = (image as? LoadedProseImage.Success)?.bitmap
        DisposableEffect(ownedBitmap) {
            onDispose {
                ownedBitmap?.takeUnless(Bitmap::isRecycled)?.recycle()
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
                color = foreground.copy(alpha = 0.06f).compositeOver(background),
                contentColor = foreground,
            ) {
                when (val loaded = image) {
                    null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (resourceLoader == null) {
                            Text(semantic.image.alternativeText?.text ?: PROSE_IMAGE_UNAVAILABLE_TEXT)
                        } else {
                            CircularProgressIndicator(color = foreground)
                        }
                    }
                    is LoadedProseImage.Success -> Image(
                        bitmap = loaded.bitmap.asImageBitmap(),
                        contentDescription = semantic.image.alternativeText?.text,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                    )
                    is LoadedProseImage.Failure -> Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(semantic.image.alternativeText?.text ?: PROSE_IMAGE_UNAVAILABLE_TEXT)
                    }
                }
            }
            semantic.caption?.let { caption ->
                ProseRichText(
                    text = caption.toSpanned(captionTypefaces),
                    documentTextIdentity = buildString {
                        append(documentTextIdentityPrefix)
                        append(":caption:")
                        append(caption.inlineStyles.hashCode())
                        append(':')
                        append(captionTypefaces.keys.sorted().joinToString())
                    },
                    textColor = foreground.copy(alpha = 0.82f).toArgbValue(),
                    textSizeSp = readerTextSizeSp * FIGURE_CAPTION_TEXT_SCALE,
                    typeface = readerTypeface,
                    lineSpacingMultiplier = lineSpacingMultiplier,
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER,
                    justificationMode = Layout.JUSTIFICATION_MODE_NONE,
                    onAnchorClick = onAnchorClick,
                    onExternalLinkClick = onExternalLinkClick,
                    anchorCharacterOffset = null,
                    onAnchorTargetPositioned = { _, _ -> },
                )
            }
        }
    }
}

internal sealed interface LoadedProseImage {
    /** The composing [ProseFigure] exclusively owns and recycles this bitmap. */
    data class Success(val bitmap: Bitmap) : LoadedProseImage
    data object Failure : LoadedProseImage
}

private suspend fun BookPublicationResourceLoader.loadProseImage(
    resourceId: String,
    targetWidthPx: Int,
    targetHeightPx: Int,
): Result<LoadedProseImage> {
    return try {
        val resource = load(
            resourceId,
            PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
            PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
        ).getOrThrow()
        Result.success(
            withContext(Dispatchers.Default) {
                LoadedProseImage.Success(
                    decodeValidatedProseImage(resource.bytes, targetWidthPx, targetHeightPx),
                )
            },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        Result.success(LoadedProseImage.Failure)
    }
}

private const val PROSE_IMAGE_UNAVAILABLE_TEXT = "Image unavailable"
internal const val MIN_IMAGE_ASPECT_RATIO = 0.25f
internal const val MAX_IMAGE_ASPECT_RATIO = 4f
internal const val DEFAULT_IMAGE_ASPECT_RATIO = 4f / 3f
private const val FIGURE_CAPTION_TEXT_SCALE = 0.875f
