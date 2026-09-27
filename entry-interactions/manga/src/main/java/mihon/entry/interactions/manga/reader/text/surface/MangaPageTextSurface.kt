package mihon.entry.interactions.manga.reader.text.surface

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import mihon.entry.interactions.manga.reader.text.image.DisplayedPageImage
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize

/**
 * A displayed reader page as seen by text features: what it shows and where it shows it.
 *
 * Window coordinates are those of the activity window, which the reader's Compose overlay shares.
 */
internal interface MangaPageTextSurface {
    val page: ReaderPage

    /** The still image currently displayed, or `null` while none is shown (loading, animated, or failed). */
    suspend fun displayedImage(): DisplayedPageImage?

    /** Where [rect] of an image of [imageSize] currently appears in the window, if the page is laid out. */
    fun imageToWindow(rect: ImageRect, imageSize: ImageSize): RectF?

    /**
     * Image coordinates of window point ([x], [y]), possibly outside the image bounds, or `null` while the page is
     * not laid out.
     */
    fun windowToImage(x: Float, y: Float, imageSize: ImageSize): Pair<Int, Int>?

    fun setTextDecoration(decoration: MangaPageTextDecoration?)
}

/** Text regions to outline on a page, in the coordinates of the recognized image. */
internal data class MangaPageTextDecoration(
    val imageSize: ImageSize,
    val regions: List<ImageRect>,
    val highlighted: ImageRect? = null,
)
