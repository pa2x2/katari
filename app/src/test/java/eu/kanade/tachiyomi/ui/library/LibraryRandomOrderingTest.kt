package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryItemKey

class LibraryRandomOrderingTest {

    @Test
    fun `hiding an entry keeps the random order of the rest`() {
        val keys = (1L..40L).map { LibraryItemKey(EntryType.MANGA, it) }
        val ordered = keys.inRandomOrder(seed = 1234)
        val hidden = ordered.first()

        val afterHiding = keys.reversed().filterNot { it == hidden }.inRandomOrder(seed = 1234)

        afterHiding shouldBe ordered.drop(1)
    }
}
