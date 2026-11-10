package eu.kanade.tachiyomi.ui.reader.viewer.strip

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.touchpadPinchSteps

/**
 * A touchpad pinch zooms the strip about the fingers' middle and moves it as they slide, as two fingers do on a touch
 * screen ([stripZoomGestures]). [scrollBy] scrolls the strip by strip pixels; [onZoomStart] is told when a pinch starts.
 */
@Composable
internal fun Modifier.stripTouchpadPinch(
    zoom: StripZoom,
    scrollBy: (Float) -> Unit,
    onZoomStart: () -> Unit,
): Modifier = touchpadPinchSteps(
    onStart = {
        zoom.stopAnimation()
        onZoomStart()
        true
    },
    onStep = { factor, focus, pan ->
        zoom.zoomBy(factor, focus, scrollBy)
        zoom.panBy(pan.x)
        scrollBy(-pan.y / zoom.scale)
    },
)
