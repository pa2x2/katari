package eu.kanade.tachiyomi.source.aniyomi.entry

import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SEntry
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AniyomiEntryConversionsTest {

    @Test
    fun `details that leave the title or URL unset, move the URL or call a season a series keep the stored identity`() {
        val stored = SEntry.create().apply {
            url = "/anime/1"
            title = "Stored title"
            type = EntryType.ANIME
        }
        // allanime fills in only what its details page has and never sets title or url
        val partial = SAnime.create().apply { description = "Fresh description" }
        // Movy marks every /tv/ URL as having seasons, its seasons' details included
        val moved = SAnime.create().apply {
            url = "https://example.org/anime/1?ref=details"
            title = "Fresh title"
            fetch_type = FetchType.Seasons
        }

        val fromPartial = partial.toSEntry(stored)
        val fromMoved = moved.toSEntry(stored)

        fromPartial.url shouldBe "/anime/1"
        fromPartial.title shouldBe "Stored title"
        fromPartial.description shouldBe "Fresh description"
        fromMoved.url shouldBe "/anime/1"
        fromMoved.title shouldBe "Fresh title"
        fromMoved.merged shouldBe false
    }
}
