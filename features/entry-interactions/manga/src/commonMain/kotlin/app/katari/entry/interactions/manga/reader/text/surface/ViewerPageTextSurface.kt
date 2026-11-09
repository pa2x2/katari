package app.katari.entry.interactions.manga.reader.text.surface

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.displayedShowing
import app.katari.entry.interactions.manga.reader.image.layout.displayedSize
import app.katari.entry.interactions.manga.reader.image.layout.pieceAt
import app.katari.entry.interactions.manga.reader.image.layout.pieces
import app.katari.entry.interactions.manga.reader.image.layout.sourcePoint
import app.katari.entry.interactions.manga.reader.text.image.DisplayedPageImage
import app.katari.entry.interactions.manga.reader.text.image.DisplayedStillImage
import app.katari.entry.interactions.manga.reader.text.image.MangaDisplayedPageImage
import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.image.ImageSize
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import okio.ByteString.Companion.toByteString
import tachiyomi.core.common.util.lang.withIOContext
import kotlin.math.roundToInt

/**
 * A page a Compose viewer shows, as text features see it. [PageTextLayer] keeps it current while the page shows a
 * still image: what is shown, and where the displayed page lies in the window. Text is read from the cropped image as
 * decoded, never cut or turned, and placed through the layout's pieces, so both halves of a split spread read the
 * same recognition. Recognition opens the page file again rather than borrowing the viewer's decoder, so it can
 * outlive the page on screen.
 */
@Stable
internal class ViewerPageTextSurface(override val page: ReaderPage) : MangaPageTextSurface {
    var shown by mutableStateOf<ShownStill?>(null)
        internal set

    /** The displayed page, laid out at the displayed size's aspect, while it is placed. */
    internal var coordinates: LayoutCoordinates? = null

    var decoration by mutableStateOf<MangaPageTextDecoration?>(null)
        private set

    /** Whether any of the page is on screen, rather than laid out beyond the viewer's edges or not shown at all. */
    val isOnScreen: Boolean
        get() = placed()?.boundsInWindow()?.let { it.width > 0f && it.height > 0f } == true

    override suspend fun displayedImage(): DisplayedPageImage? {
        val still = shown ?: return null
        val stream = page.stream ?: return null
        val bytes = withIOContext { stream().use { it.readBytes() } }
        return MangaDisplayedPageImage.open(
            DisplayedStillImage(bytes.toByteString(), still.cropBorders, rawContent = null),
        )
    }

    // Every piece showing part of the rectangle counts, so the whole image of a stacked spread spans both halves
    override fun imageToWindow(rect: ImageRect, imageSize: ImageSize): Rect? {
        val still = shown ?: return null
        val coordinates = placed() ?: return null
        val image = still.toImage(rect, imageSize)
        val displayed = still.pieces.mapNotNull { it.displayedShowing(image) }.reduceOrNull(IntRect::spanning)
            ?: return null
        val scale = still.scaleIn(coordinates.size)
        val first = coordinates.localToWindow(Offset(displayed.left * scale, displayed.top * scale))
        val second = coordinates.localToWindow(Offset(displayed.right * scale, displayed.bottom * scale))
        return Rect(
            left = minOf(first.x, second.x),
            top = minOf(first.y, second.y),
            right = maxOf(first.x, second.x),
            bottom = maxOf(first.y, second.y),
        )
    }

    override fun windowToImage(x: Float, y: Float, imageSize: ImageSize): Pair<Int, Int>? {
        val still = shown ?: return null
        val coordinates = placed() ?: return null
        val scale = still.scaleIn(coordinates.size).takeIf { it > 0f } ?: return null
        val displayed = coordinates.windowToLocal(Offset(x, y)) / scale
        val point = still.pieces.pieceAt(displayed)?.sourcePoint(displayed) ?: return null
        return (point.x * imageSize.width / still.imageSize.width).roundToInt() to
            (point.y * imageSize.height / still.imageSize.height).roundToInt()
    }

    override fun setTextDecoration(decoration: MangaPageTextDecoration?) {
        this.decoration = decoration
    }

    private fun placed(): LayoutCoordinates? = coordinates?.takeIf { shown != null && it.isAttached }
}

/** A still image of [imageSize] in pixels, borders cropped when [cropBorders] says, shown in [layout]. */
internal class ShownStill(val imageSize: IntSize, val layout: PageImageLayout, val cropBorders: Boolean) {
    val pieces = layout.pieces(imageSize)
    private val displayedSize = layout.displayedSize(imageSize)

    /** Pixels of a box of [boxSize] showing the displayed page, per pixel of the displayed page. */
    fun scaleIn(boxSize: IntSize): Float =
        if (displayedSize.width > 0) boxSize.width.toFloat() / displayedSize.width else 0f

    /** [rect] of a recognized image of [recognizedSize], in this image's pixels. */
    fun toImage(rect: ImageRect, recognizedSize: ImageSize): IntRect {
        val scaleX = imageSize.width.toFloat() / recognizedSize.width
        val scaleY = imageSize.height.toFloat() / recognizedSize.height
        return IntRect(
            (rect.left * scaleX).roundToInt(),
            (rect.top * scaleY).roundToInt(),
            (rect.right * scaleX).roundToInt(),
            (rect.bottom * scaleY).roundToInt(),
        )
    }

    /**
     * Where [rect] of a recognized image of [recognizedSize] appears in a box of [boxSize] showing the displayed page:
     * on the piece showing most of it, clipped to that piece, or null when no piece shows it.
     */
    fun inBox(rect: ImageRect, recognizedSize: ImageSize, boxSize: IntSize): Rect? {
        val image = toImage(rect, recognizedSize)
        val displayed = pieces.mapNotNull { it.displayedShowing(image) }
            .maxByOrNull { it.width.toLong() * it.height }
            ?: return null
        val scale = scaleIn(boxSize)
        return Rect(displayed.left * scale, displayed.top * scale, displayed.right * scale, displayed.bottom * scale)
    }
}

private fun IntRect.spanning(other: IntRect) = IntRect(
    minOf(left, other.left),
    minOf(top, other.top),
    maxOf(right, other.right),
    maxOf(bottom, other.bottom),
)
