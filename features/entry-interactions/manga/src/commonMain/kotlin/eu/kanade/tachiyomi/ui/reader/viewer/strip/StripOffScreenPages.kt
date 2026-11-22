package eu.kanade.tachiyomi.ui.reader.viewer.strip

import app.katari.entry.interactions.manga.reader.image.profile.DisplayProfile
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import app.katari.entry.interactions.manga.reader.image.source.openPageImage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import java.io.InputStream

/**
 * Opens the image of [page], which the strip may not have composed, as the strip shows it with [cropBorders] once its
 * chapter's loader has it, or returns null when it fails to load or can't be read. The caller closes the image.
 */
internal suspend fun openOffScreenPage(page: ReaderPage, cropBorders: Boolean): PageImage? {
    val stream = page.readyStream() ?: return null
    val bytes = withIOContext { stream().use { it.readBytes() } }
    return openPageImage(bytes, cropBorders, DisplayProfile.bytes)
}

/**
 * The stream of the page's image once its chapter's loader has it, asking the loader for it alone, or null when it
 * fails to load.
 */
private suspend fun ReaderPage.readyStream(): (() -> InputStream)? {
    if (status == Page.State.Ready) return stream
    val loader = chapter.pageLoader ?: return null
    return coroutineScope {
        val loading = launchIO { loader.loadPage(this@readyStream, 0) }
        val state = statusFlow.first { it == Page.State.Ready || it is Page.State.Error }
        loading.cancel()
        stream.takeIf { state == Page.State.Ready }
    }
}
