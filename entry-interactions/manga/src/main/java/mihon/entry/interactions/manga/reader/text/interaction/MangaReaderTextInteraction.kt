package mihon.entry.interactions.manga.reader.text.interaction

import android.graphics.RectF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextSession
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextSurface
import mihon.entry.interactions.manga.reader.text.translation.MangaTextTranslationController
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextRecognitionResult
import mihon.translation.ui.session.TranslationSelectionAnchor

/**
 * Turns reader gestures into text actions: a tap on a recognized region or an outlined area is translated with the
 * translation popup anchored where that text appears.
 *
 * Coordinates are window coordinates. [overlayOrigin] is the window position of the layer that hosts the popup.
 */
internal class MangaReaderTextInteraction(
    private val session: MangaReaderTextSession,
    private val translation: MangaTextTranslationController,
    private val visibleSurfaces: () -> List<MangaPageTextSurface>,
    private val overlayOrigin: () -> Pair<Float, Float>,
    private val scope: CoroutineScope,
) {
    private val mutableAreaResult = MutableStateFlow<AreaResult?>(null)

    /** Outcome of the last outlined area, for hosts to report areas that contained no text. */
    val areaResult: StateFlow<AreaResult?> = mutableAreaResult.asStateFlow()

    fun setActive(active: Boolean) {
        session.setActive(active)
        if (active) {
            refreshSurfaces()
        } else {
            dismissTranslation()
        }
    }

    /** Recognizes the pages currently on screen; called whenever they or their images change. */
    fun refreshSurfaces() {
        if (session.state.value.active) session.onVisibleSurfaces(visibleSurfaces())
    }

    /**
     * Handles a reader tap at window point ([x], [y]). Returns whether the tap was consumed; taps outside recognized
     * text keep their normal reader meaning unless they dismiss an open translation.
     */
    fun onTap(x: Float, y: Float): Boolean {
        if (!session.state.value.active) return false
        visibleSurfaces().forEach { surface ->
            val result = session.result(surface.page) ?: return@forEach
            val (imageX, imageY) = surface.windowToImage(x, y, result.imageSize) ?: return@forEach
            val region = session.regionAt(surface.page, imageX, imageY) ?: return@forEach
            translateRegion(surface, result, region)
            return true
        }
        if (translation.isActive) {
            dismissTranslation()
            return true
        }
        return false
    }

    /** Recognizes and translates the text inside [area], a rectangle the user outlined in window coordinates. */
    fun onAreaSelected(area: RectF) {
        scope.launch {
            val (surface, imageSize) = visibleSurfaces().firstNotNullOfOrNull { surface ->
                val size = surface.displayedImage()?.use { it.size } ?: return@firstNotNullOfOrNull null
                val bounds = surface.imageToWindow(size.bounds, size) ?: return@firstNotNullOfOrNull null
                (surface to size).takeIf { bounds.contains(area.centerX(), area.centerY()) }
            } ?: return@launch
            val topLeft = surface.windowToImage(area.left, area.top, imageSize) ?: return@launch
            val bottomRight = surface.windowToImage(area.right, area.bottom, imageSize) ?: return@launch
            val imageArea = clampedArea(topLeft, bottomRight, imageSize.width, imageSize.height) ?: return@launch
            val result = session.recognizeArea(surface, imageArea)
            // A missing prerequisite is reported by the session itself.
            if (result == null) return@launch
            if (result.regions.isEmpty()) {
                mutableAreaResult.value = AreaResult.NoText
                return@launch
            }
            mutableAreaResult.value = AreaResult.Translated
            session.highlight(surface.page, imageArea)
            translation.translate(
                text = result.regions.joinToString("\n") { it.text },
                language = result.language,
                pageText = session.result(surface.page)?.pageText().orEmpty(),
                anchor = surface.imageToWindow(imageArea, imageSize)?.let(::anchor),
            )
        }
    }

    fun consumeAreaResult() {
        mutableAreaResult.value = null
    }

    fun dismissTranslation() {
        translation.dismiss()
        session.clearHighlight()
    }

    private fun translateRegion(
        surface: MangaPageTextSurface,
        result: TextRecognitionResult,
        region: RecognizedTextRegion,
    ) {
        val bounds = region.container ?: region.bounds
        session.highlight(surface.page, bounds)
        translation.translate(
            text = region.text,
            language = result.language,
            pageText = result.pageText(),
            anchor = surface.imageToWindow(bounds, result.imageSize)?.let(::anchor),
        )
    }

    private fun anchor(window: RectF): TranslationSelectionAnchor {
        val (originX, originY) = overlayOrigin()
        return TranslationSelectionAnchor(
            left = window.left - originX,
            top = window.top - originY,
            right = window.right - originX,
            bottom = window.bottom - originY,
        )
    }

    private fun TextRecognitionResult.pageText(): String = regions.joinToString("\n") { it.text }

    enum class AreaResult {
        Translated,
        NoText,
    }
}

/** The image rectangle spanned by two outline corners, clipped to the image; `null` when nothing remains. */
internal fun clampedArea(
    first: Pair<Int, Int>,
    second: Pair<Int, Int>,
    width: Int,
    height: Int,
): ImageRect? {
    val (x1, y1) = first
    val (x2, y2) = second
    val left = minOf(x1, x2).coerceIn(0, width)
    val right = maxOf(x1, x2).coerceIn(0, width)
    val top = minOf(y1, y2).coerceIn(0, height)
    val bottom = maxOf(y1, y2).coerceIn(0, height)
    return if (right > left && bottom > top) ImageRect(left, top, right, bottom) else null
}
