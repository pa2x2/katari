package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * An [AnimeHttpSource] that parses HTML pages with CSS selectors. The video selectors are extensions-lib 14's;
 * later libs dropped them, so they are not abstract here.
 */
abstract class ParsedAnimeHttpSource : AnimeHttpSource() {

    override fun popularAnimeParse(response: Response): AnimesPage =
        parsePage(response, popularAnimeSelector(), popularAnimeNextPageSelector(), ::popularAnimeFromElement)

    protected abstract fun popularAnimeSelector(): String

    protected abstract fun popularAnimeFromElement(element: Element): SAnime

    protected abstract fun popularAnimeNextPageSelector(): String?

    override fun searchAnimeParse(response: Response): AnimesPage =
        parsePage(response, searchAnimeSelector(), searchAnimeNextPageSelector(), ::searchAnimeFromElement)

    protected abstract fun searchAnimeSelector(): String

    protected abstract fun searchAnimeFromElement(element: Element): SAnime

    protected abstract fun searchAnimeNextPageSelector(): String?

    override fun latestUpdatesParse(response: Response): AnimesPage =
        parsePage(response, latestUpdatesSelector(), latestUpdatesNextPageSelector(), ::latestUpdatesFromElement)

    protected abstract fun latestUpdatesSelector(): String

    protected abstract fun latestUpdatesFromElement(element: Element): SAnime

    protected abstract fun latestUpdatesNextPageSelector(): String?

    override fun animeDetailsParse(response: Response): SAnime = animeDetailsParse(response.document())

    protected abstract fun animeDetailsParse(document: Document): SAnime

    override fun relatedAnimeListParse(response: Response): List<SAnime> =
        response.document().select(relatedAnimeListSelector()).map(::relatedAnimeFromElement)

    protected open fun relatedAnimeListSelector(): String = popularAnimeSelector()

    protected open fun relatedAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)

    override fun episodeListParse(response: Response): List<SEpisode> =
        response.document().select(episodeListSelector()).map(::episodeFromElement)

    protected abstract fun episodeListSelector(): String

    protected abstract fun episodeFromElement(element: Element): SEpisode

    override fun seasonListParse(response: Response): List<SAnime> =
        response.document().select(seasonListSelector()).map(::seasonFromElement)

    // extensions-lib 16 and later declare these two abstract, but extensions built against 14 don't implement them;
    // a body turns a season call on such a source into an exception instead of an AbstractMethodError
    protected open fun seasonListSelector(): String = throw UnsupportedOperationException()

    protected open fun seasonFromElement(element: Element): SAnime = throw UnsupportedOperationException()

    override fun videoListParse(response: Response): List<Video> =
        response.document().select(videoListSelector()).map(::videoFromElement)

    protected open fun videoListSelector(): String = throw UnsupportedOperationException()

    protected open fun videoFromElement(element: Element): Video = throw UnsupportedOperationException()

    override fun videoUrlParse(response: Response): String = videoUrlParse(response.document())

    protected open fun videoUrlParse(document: Document): String = throw UnsupportedOperationException()

    private fun Response.document(): Document = Jsoup.parse(body.string(), request.url.toString())

    private fun parsePage(
        response: Response,
        selector: String,
        nextPageSelector: String?,
        fromElement: (Element) -> SAnime,
    ): AnimesPage {
        val document = response.document()
        val animes = document.select(selector).map(fromElement)
        val hasNextPage = nextPageSelector?.let { document.select(it).first() } != null
        return AnimesPage(animes, hasNextPage)
    }
}
