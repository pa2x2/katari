package mihon.entry.interactions.manga.reader.text.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.flow.Flow

/**
 * The text surfaces of the pages a Compose viewer has composed: those on screen are visible, and those laid out around
 * them with an image shown are preloaded. Each surface draws what [decorations] says for its page.
 */
internal class ViewerTextSurfaces(private val decorations: (ReaderPage) -> Flow<MangaPageTextDecoration?>) {
    private val surfaces = LinkedHashSet<ViewerPageTextSurface>()

    fun visible(): List<MangaPageTextSurface> = surfaces.filter { it.isOnScreen }

    fun preloaded(): List<MangaPageTextSurface> = surfaces.filter { !it.isOnScreen && it.shown != null }

    /** The surface of [page] while the calling composable shows it. */
    @Composable
    fun rememberSurface(page: ReaderPage): ViewerPageTextSurface {
        val surface = remember(page) { ViewerPageTextSurface(page) }
        DisposableEffect(surface) {
            surfaces += surface
            onDispose { surfaces -= surface }
        }
        LaunchedEffect(surface) { decorations(page).collect(surface::setTextDecoration) }
        return surface
    }
}
