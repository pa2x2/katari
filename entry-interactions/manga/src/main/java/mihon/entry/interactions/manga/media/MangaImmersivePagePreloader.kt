package mihon.entry.interactions.manga.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CancellationException
import kotlin.math.min

private const val MANGA_IMMERSIVE_PRELOAD_COUNT = 10

@Composable
internal fun MangaImmersivePagePreloader(
    pages: List<MangaImmersivePage>,
    currentPage: Int,
    active: Boolean,
) {
    LaunchedEffect(pages, currentPage, active) {
        if (!active) return@LaunchedEffect
        mangaImmersivePreloadIndexes(currentPage, pages.size).forEach { pageIndex ->
            try {
                pages[pageIndex].loadImage()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // A failed preload remains retryable when its page becomes visible.
            }
        }
    }
}

internal fun mangaImmersivePreloadIndexes(
    currentPage: Int,
    pageCount: Int,
): IntRange {
    if (currentPage !in 0 until pageCount || currentPage == pageCount - 1) return IntRange.EMPTY
    return (currentPage + 1)..min(currentPage + MANGA_IMMERSIVE_PRELOAD_COUNT, pageCount - 1)
}
