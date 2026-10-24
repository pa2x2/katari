package eu.kanade.tachiyomi.source.aniyomi.entry

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SEntry
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AniyomiEntryConversionsTest {

    @Test
    fun `details that leave the title or URL unset, or move the URL, keep the stored title and identity`() {
        val stored = SEntry.create().apply {
            url = "/anime/1"
            title = "Stored title"
            type = EntryType.ANIME
        }
        // allanime fills in only what its details page has and never sets title or url
        val partial = SAnime.create().apply { description = "Fresh description" }
        val moved = SAnime.create().apply {
            url = "https://example.org/anime/1?ref=details"
            title = "Fresh title"
        }

        val fromPartial = partial.toSEntry(stored)
        val fromMoved = moved.toSEntry(stored)

        fromPartial.url shouldBe "/anime/1"
        fromPartial.title shouldBe "Stored title"
        fromPartial.description shouldBe "Fresh description"
        fromMoved.url shouldBe "/anime/1"
        fromMoved.title shouldBe "Fresh title"
    }
}
