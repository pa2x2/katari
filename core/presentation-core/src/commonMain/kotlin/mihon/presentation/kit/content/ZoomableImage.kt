package mihon.presentation.kit.content

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import kotlin.math.pow
import coil3.size.Size as ImageSize

/**
 * The image [model] names, fitted inside [contentPadding], that zooms in and out around the pointer with the mouse
 * wheel or a pinch and in or back out with a double click or tap, and pans with a drag while zoomed. At its fitted
 * size a drag is left to the container, such as a pager. A single click or tap calls [onClick]. The image loads at its
 * full size, so it stays sharp zoomed in.
 */
@Composable
fun ZoomableImage(
    model: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val zoom = remember { ZoomState() }
    // Another image starts at its fitted size
    LaunchedEffect(model) { zoom.reset() }
    val currentOnClick by rememberUpdatedState(onClick)
    val context = LocalPlatformContext.current
    val request = remember(context, model) {
        ImageRequest.Builder(context)
            .data(model)
            .size(ImageSize.ORIGINAL)
            .build()
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { zoom.size = it }
            .pointerInput(zoom) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val change = event.changes.first()
                        // A wheel notch scrolls by 1, a touchpad by fractions of it
                        zoom.zoomTo(zoom.scale * WHEEL_ZOOM_PER_NOTCH.pow(-change.scrollDelta.y), change.position)
                        change.consume()
                    }
                }
            }
            .pointerInput(zoom) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var dragged = Offset.Zero
                    var transforming = false
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.count { it.pressed }
                        dragged += event.calculatePan()
                        // A pinch zooms at once; one pointer pans once it has moved past the slop, so a click that
                        // jitters still counts as a click
                        if (!transforming) {
                            transforming = pointers > 1 ||
                                (zoom.scale > 1f && dragged.getDistance() > viewConfiguration.touchSlop)
                        }
                        if (transforming && (pointers > 1 || zoom.scale > 1f)) {
                            val zoomChange = event.calculateZoom()
                            if (zoomChange != 1f) zoom.zoomTo(zoom.scale * zoomChange, event.calculateCentroid())
                            zoom.panBy(event.calculatePan())
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(zoom) {
                detectTapGestures(
                    onDoubleTap = { position ->
                        if (zoom.scale > 1f) zoom.reset() else zoom.zoomTo(DOUBLE_TAP_SCALE, position)
                    },
                    onTap = { currentOnClick() },
                )
            },
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onSuccess = { zoom.imageSize = it.painter.intrinsicSize },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offset.x
                    translationY = zoom.offset.y
                }
                .padding(contentPadding)
                .onSizeChanged { zoom.fitArea = it },
        )
    }
}

/**
 * How far the image is zoomed, and moved from the centre, within a box of [size]. The image of [imageSize] is fitted
 * into [fitArea], the box less its content padding.
 */
private class ZoomState {
    var size: IntSize = IntSize.Zero
    var fitArea: IntSize = IntSize.Zero
    var imageSize: Size = Size.Unspecified
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** Zooms to [target], keeping the point of the image under [focus] where it is. */
    fun zoomTo(target: Float, focus: Offset) {
        val newScale = target.coerceIn(1f, MAX_SCALE)
        val fromCentre = focus - Offset(size.width / 2f, size.height / 2f)
        offset = clamped(fromCentre - (fromCentre - offset) * (newScale / scale), newScale)
        scale = newScale
    }

    fun panBy(pan: Offset) {
        offset = clamped(offset + pan, scale)
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    // The image moves only by the part of it that lies outside the box, and stays centred along a side it doesn't fill
    private fun clamped(offset: Offset, scale: Float): Offset {
        val image = fittedImage()
        val maxX = ((image.width * scale - size.width) / 2f).coerceAtLeast(0f)
        val maxY = ((image.height * scale - size.height) / 2f).coerceAtLeast(0f)
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    // Until the image has loaded, as if it filled the box
    private fun fittedImage(): Size {
        if (imageSize.isUnspecified || imageSize.width <= 0f || imageSize.height <= 0f) {
            return Size(size.width.toFloat(), size.height.toFloat())
        }
        val fit = minOf(fitArea.width / imageSize.width, fitArea.height / imageSize.height)
        return imageSize * fit
    }
}

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
private const val WHEEL_ZOOM_PER_NOTCH = 1.25f
