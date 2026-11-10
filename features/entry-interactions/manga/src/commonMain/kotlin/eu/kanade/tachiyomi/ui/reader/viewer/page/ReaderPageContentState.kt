package eu.kanade.tachiyomi.ui.reader.viewer.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

/**
 * What a composed page shows, owning the image it opened: an image no longer shown, because the page loaded again or
 * left the composition, is closed.
 */
@Stable
internal class ReaderPageContentState {
    var content by mutableStateOf<PageContent>(PageContent.Loading(null))
        private set

    internal fun show(next: PageContent) {
        val previous = content
        content = next
        if (previous is PageContent.Shown && previous.image !== (next as? PageContent.Shown)?.image) {
            previous.image.close()
        }
    }

    /** Shows the page as failed, as when its image turns out to be unreadable while drawing. */
    fun fail() = show(PageContent.Failed(null))
}

/**
 * Loads [page] while composed, again when [settingsKey] changes, as when a setting that shapes the image does; the
 * request is read from [request] each time the image is ready. [onShown] hears each time the image is shown, for the
 * reader's session report. [preloadCount] is as [loadReaderPage] takes it.
 */
@Composable
internal fun rememberReaderPageContent(
    page: ReaderPage,
    settingsKey: Any?,
    request: () -> PageRequest,
    onShown: (ReaderPage) -> Unit = {},
    preloadCount: Int? = null,
): ReaderPageContentState {
    val state = remember(page) { ReaderPageContentState() }
    LaunchedEffect(state, settingsKey) {
        loadReaderPage(
            page = page,
            request = request,
            onContent = { content ->
                state.show(content)
                if (content is PageContent.Shown) onShown(page)
            },
            preloadCount = preloadCount,
        )
    }
    DisposableEffect(state) { onDispose { (state.content as? PageContent.Shown)?.image?.close() } }
    return state
}
