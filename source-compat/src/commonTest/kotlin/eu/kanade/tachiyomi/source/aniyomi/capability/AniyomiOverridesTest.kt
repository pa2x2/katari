package eu.kanade.tachiyomi.source.aniyomi.capability

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import io.kotest.matchers.shouldBe
import okhttp3.Response
import org.junit.jupiter.api.Test

class AniyomiOverridesTest {

    private open class Site : AnimeHttpSource() {
        override val name = "Site"
        override val lang = "en"
        override val baseUrl = "https://site.example"
        override val supportsLatest = false
    }

    private class SiteListingRelated : Site() {
        override fun relatedAnimeListParse(response: Response): List<SAnime> = emptyList()
    }

    @Test
    fun `only an extension that lists related titles itself gets them, not every site through Komikku's defaults`() {
        // Komikku's base class says every website source supports related titles
        Site().relatedTitlesCall shouldBe null
        SiteListingRelated().relatedTitlesCall shouldBe RelatedTitlesCall.FETCHED
    }
}
