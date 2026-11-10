package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.gesture.touchpadPinchSteps

/**
 * A touchpad pinch zooms the page it starts over about the fingers' middle and moves it as they slide, as two fingers
 * do on a touch screen ([pagerPageGestures]). [pageAt] finds the page at a point of the viewer and where it lies there.
 */
@Composable
internal fun Modifier.pagerTouchpadPinch(
    viewer: PagedViewer,
    pageAt: (Offset) -> Pair<ReaderPage, Rect>?,
): Modifier {
    val pinch = remember(viewer) { PinchedPage() }
    return touchpadPinchSteps(
        onStart = { pointer ->
            val (page, bounds) = pageAt(pointer) ?: return@touchpadPinchSteps false
            pinch.zoom = viewer.zoomOf(page)
            pinch.pageTopLeft = bounds.topLeft
            pinch.zoom != null
        },
        onStep = { factor, focus, pan ->
            val zoom = pinch.zoom ?: return@touchpadPinchSteps
            if (factor != 1f) {
                zoom.zoomBy(factor, focus - pinch.pageTopLeft)
                viewer.onPageZoomed()
            }
            zoom.panBy(pan)
        },
    )
}

private class PinchedPage {
    var zoom: PagerPageZoom? = null
    var pageTopLeft = Offset.Zero
}
