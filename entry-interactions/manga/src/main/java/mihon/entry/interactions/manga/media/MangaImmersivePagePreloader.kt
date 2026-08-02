package mihon.entry.interactions.manga.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlin.math.min

private const val MANGA_IMMERSIVE_PRELOAD_COUNT = 10

@Composable
internal fun MangaImmersivePagePreloader(
    pages: List<ReaderPage>,
    currentPage: Int,
    active: Boolean,
) {
    val pageLoader = pages.firstOrNull()?.chapter?.pageLoader
    LaunchedEffect(pageLoader, pages, currentPage, active) {
        val preloadPages = if (active) {
            mangaImmersivePreloadIndexes(currentPage, pages.size).map(pages::get)
        } else {
            emptyList()
        }
        pageLoader?.setPreloadPages(preloadPages)
    }
    DisposableEffect(pageLoader) {
        onDispose { pageLoader?.setPreloadPages(emptyList()) }
    }
}

internal fun mangaImmersivePreloadIndexes(
    currentPage: Int,
    pageCount: Int,
): IntRange {
    if (currentPage !in 0 until pageCount || currentPage == pageCount - 1) return IntRange.EMPTY
    return (currentPage + 1)..min(currentPage + MANGA_IMMERSIVE_PRELOAD_COUNT, pageCount - 1)
}
