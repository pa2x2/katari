package mihon.entry.interactions.manga.media

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class MangaImmersivePagePreloaderTest {
    @Test
    fun `preload window contains only the following ten available pages`() {
        mangaImmersivePreloadIndexes(currentPage = 3, pageCount = 30).toList()
            .shouldContainExactly((4..13).toList())
        mangaImmersivePreloadIndexes(currentPage = 25, pageCount = 30).toList()
            .shouldContainExactly((26..29).toList())
        mangaImmersivePreloadIndexes(currentPage = 29, pageCount = 30).toList()
            .shouldContainExactly(emptyList())
    }
}
