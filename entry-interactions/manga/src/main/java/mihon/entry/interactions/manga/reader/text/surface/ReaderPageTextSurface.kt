package mihon.entry.interactions.manga.reader.text.surface

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import mihon.entry.interactions.manga.reader.text.image.DisplayedPageImage
import mihon.entry.interactions.manga.reader.text.image.MangaDisplayedPageImage
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize

/** A page view of either viewer, exposed as a text surface for the page it currently shows. */
internal class ReaderPageTextSurface(
    override val page: ReaderPage,
    private val view: ReaderPageImageView,
) : MangaPageTextSurface {

    override suspend fun displayedImage(): DisplayedPageImage? {
        val (encoded, cropBorders) = view.displayedStillImage() ?: return null
        return MangaDisplayedPageImage.open(encoded, cropBorders)
    }

    override fun imageToWindow(rect: ImageRect, imageSize: ImageSize): RectF? = view.imageToWindow(rect, imageSize)

    override fun windowToImage(x: Float, y: Float, imageSize: ImageSize): Pair<Int, Int>? =
        view.windowToImage(x, y, imageSize)

    override fun setTextDecoration(decoration: MangaPageTextDecoration?) = view.setTextDecoration(decoration)
}
