package eu.kanade.tachiyomi.source.aniyomi

import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.source.entry.ChapterWebViewSource
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AniyomiWebBridgeTest {

    private open class Site : AnimeHttpSource() {
        override val name = "Site"
        override val lang = "en"
        override val baseUrl = "https://site.example"
        override val supportsLatest = false
    }

    private class SiteWithEpisodePages : Site() {
        override fun getEpisodeUrl(episode: SEpisode): String = "https://watch.example${episode.url}"
    }

    @Test
    fun `an episode stored without the domain opens on the source's site, and an extension's own address is kept`() {
        val episode = SEntryChapter.create().apply {
            url = "/show-af88/ep-1-ecbaee"
            name = "Ep. 1"
        }

        (Site().toUnifiedSource(1L) as ChapterWebViewSource).getChapterUrl(episode) shouldBe
            "https://site.example/show-af88/ep-1-ecbaee"
        (SiteWithEpisodePages().toUnifiedSource(2L) as ChapterWebViewSource).getChapterUrl(episode) shouldBe
            "https://watch.example/show-af88/ep-1-ecbaee"
    }
}
