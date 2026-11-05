package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.compose.ui.unit.IntSize
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import mihon.entry.interactions.manga.reader.image.background.PageBackground
import mihon.entry.interactions.manga.reader.image.background.chooseBackground
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.image.source.PageImage
import mihon.entry.interactions.manga.reader.image.source.openPageImage
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat

/** What a page slot shows while its page loads, once it is ready, or when it failed. */
internal sealed interface PagerPageContent {
    /** [progress] of the download from 0 to 1, or null before it starts. */
    data class Loading(val progress: Float?) : PagerPageContent

    /** [background] is what surrounds the page when the background follows the page, else null. */
    data class Shown(
        val image: PageImage,
        val layout: PageImageLayout,
        val background: PageBackground?,
    ) : PagerPageContent

    data class Failed(val error: Throwable?) : PagerPageContent
}

/** What opening a page needs from the viewer, read when the page's image is ready. */
internal class PagerPageRequest(
    val cropBorders: Boolean,
    val automaticBackground: Boolean,
    val landscape: Boolean,
    /** Decides the page's layout from its image size, and tells the viewer what that means for the pages around it. */
    val layout: (ReaderPage, imageSize: IntSize) -> PageImageLayout,
)

/**
 * Loads [page] through its chapter's loader and reports what the slot shows through [onContent] until cancelled. The
 * opened image is handed over with [PagerPageContent.Shown]; the receiver closes it once it no longer shows it.
 * [request] is read again each time the page becomes ready, so a retry follows settings changed since.
 */
internal suspend fun loadPagerPage(
    page: ReaderPage,
    request: () -> PagerPageRequest,
    onContent: (PagerPageContent) -> Unit,
) {
    val loader = page.chapter.pageLoader ?: return
    supervisorScope {
        launchIO { loader.loadPage(page) }
        page.statusFlow.collectLatest { state ->
            when (state) {
                Page.State.Queue, Page.State.LoadPage -> onContent(PagerPageContent.Loading(null))
                Page.State.DownloadImage -> page.progressFlow.collectLatest {
                    onContent(PagerPageContent.Loading(it / 100f))
                }
                Page.State.Ready -> onContent(openShown(page, request()))
                is Page.State.Error -> onContent(PagerPageContent.Failed(state.error))
            }
        }
    }
}

private suspend fun openShown(page: ReaderPage, request: PagerPageRequest): PagerPageContent {
    val stream = page.stream ?: return PagerPageContent.Failed(null)
    var image: PageImage? = null
    return try {
        val bytes = withIOContext { stream().use { it.readBytes() } }
        image = openPageImage(bytes, request.cropBorders, TachiyomiImageDecoder.displayProfile)
            ?: return PagerPageContent.Failed(null)
        val layout = request.layout(page, image.size)
        val background = if (request.automaticBackground && image is PageImage.Still) {
            image.chooseBackground(layout, request.landscape)
        } else {
            null
        }
        PagerPageContent.Shown(image, layout, background)
    } catch (e: Throwable) {
        image?.close()
        if (e is CancellationException) throw e
        page.logcat(LogPriority.ERROR, e) { "Page ${page.number} could not be opened" }
        PagerPageContent.Failed(e)
    }
}
