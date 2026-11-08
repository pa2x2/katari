package eu.kanade.tachiyomi.animesource.online

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.Hoster
import eu.kanade.tachiyomi.animesource.model.HttpServer
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.ThumbnailInfo
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

/**
 * The base class of Aniyomi sources that talk to a website. Each `get*` call sends the matching `*Request` and
 * hands the response to `*Parse`; extensions override the pairs they use. The calls of every extensions-lib
 * version stay, because compiled extensions override them.
 */
abstract class AnimeHttpSource : AnimeCatalogueSource {

    protected val network: NetworkHelper by injectLazy()

    /** The site's address without a trailing slash. */
    abstract val baseUrl: String

    open fun getHomeUrl(): String = baseUrl

    /** Raised by an extension when its source must get a new [id], which turns it into a new source. */
    open val versionId: Int = 1

    override val id: Long by lazy { generateId(name, lang, versionId) }

    val headers: Headers by lazy { headersBuilder().build() }

    open val client: OkHttpClient
        get() = network.client

    protected fun generateId(name: String, lang: String, versionId: Int): Long {
        val key = "${name.lowercase()}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    protected open fun headersBuilder(): Headers.Builder = Headers.Builder().apply {
        add("User-Agent", network.defaultUserAgentProvider())
    }

    override fun toString(): String = "$name (${lang.uppercase()})"

    override suspend fun getPopularAnime(page: Int): AnimesPage =
        client.newCall(popularAnimeRequest(page)).awaitSuccess().let(::popularAnimeParse)

    protected open fun popularAnimeRequest(page: Int): Request = throw UnsupportedOperationException()

    protected open fun popularAnimeParse(response: Response): AnimesPage = throw UnsupportedOperationException()

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage =
        client.newCall(searchAnimeRequest(page, query, filters)).awaitSuccess().let(::searchAnimeParse)

    protected open fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        throw UnsupportedOperationException()

    protected open fun searchAnimeParse(response: Response): AnimesPage = throw UnsupportedOperationException()

    override suspend fun getLatestUpdates(page: Int): AnimesPage =
        client.newCall(latestUpdatesRequest(page)).awaitSuccess().let(::latestUpdatesParse)

    protected open fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()

    protected open fun latestUpdatesParse(response: Response): AnimesPage = throw UnsupportedOperationException()

    override suspend fun getAnimeDetails(anime: SAnime): SAnime =
        client.newCall(animeDetailsRequest(anime)).awaitSuccess()
            .let { animeDetailsParse(it).apply { initialized = true } }

    open fun animeDetailsRequest(anime: SAnime): Request = GET(baseUrl + anime.url, headers)

    protected open fun animeDetailsParse(response: Response): SAnime = throw UnsupportedOperationException()

    // Komikku's fork: related entries the site lists on an anime's page
    override val supportsRelatedAnimes: Boolean
        get() = true

    override suspend fun fetchRelatedAnimeList(anime: SAnime): List<SAnime> =
        client.newCall(relatedAnimeListRequest(anime)).awaitSuccess().let(::relatedAnimeListParse)

    protected open fun relatedAnimeListRequest(anime: SAnime): Request = animeDetailsRequest(anime)

    protected open fun relatedAnimeListParse(response: Response): List<SAnime> = popularAnimeParse(response).animes

    override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> =
        client.newCall(episodeListRequest(anime)).awaitSuccess().let(::episodeListParse)

    protected open fun episodeListRequest(anime: SAnime): Request = GET(baseUrl + anime.url, headers)

    protected open fun episodeListParse(response: Response): List<SEpisode> = throw UnsupportedOperationException()

    protected open fun episodeVideoParse(response: Response): SEpisode = throw UnsupportedOperationException()

    override suspend fun getSeasonList(anime: SAnime): List<SAnime> =
        client.newCall(seasonListRequest(anime)).awaitSuccess().let(::seasonListParse)

    protected open fun seasonListRequest(anime: SAnime): Request = GET(baseUrl + anime.url, headers)

    protected open fun seasonListParse(response: Response): List<SAnime> = throw UnsupportedOperationException()

    override suspend fun getHosterList(episode: SEpisode): List<Hoster> =
        client.newCall(hosterListRequest(episode)).awaitSuccess().let(::hosterListParse)

    protected open fun hosterListRequest(episode: SEpisode): Request = GET(baseUrl + episode.url, headers)

    protected open fun hosterListParse(response: Response): List<Hoster> = throw UnsupportedOperationException()

    override suspend fun getVideoList(hoster: Hoster): List<Video> =
        client.newCall(videoListRequest(hoster)).awaitSuccess().let { videoListParse(it, hoster) }

    protected open fun videoListRequest(hoster: Hoster): Request = GET(hoster.hosterUrl, headers)

    protected open fun videoListParse(response: Response, hoster: Hoster): List<Video> =
        throw UnsupportedOperationException()

    override suspend fun getVideoList(episode: SEpisode): List<Video> =
        client.newCall(videoListRequest(episode)).awaitSuccess().let(::videoListParse)

    protected open fun videoListRequest(episode: SEpisode): Request = GET(baseUrl + episode.url, headers)

    protected open fun videoListParse(response: Response): List<Video> = throw UnsupportedOperationException()

    /** Fills in a video the extension returned unresolved; null when it can't be played. */
    open suspend fun resolveVideo(video: Video): Video? = video

    /** A server the host starts before playing videos that point at [HttpServer.PLACEHOLDER_URL]. */
    open fun createHttpServer(): HttpServer? = null

    open suspend fun getVideoThumbnails(video: Video): ThumbnailInfo? = null

    open suspend fun getImageTile(url: String): Bitmap? =
        client.newCall(GET(url, headers)).awaitSuccess().body.byteStream().use(BitmapFactory::decodeStream)

    open fun List<Hoster>.sortHosters(): List<Hoster> = this

    open fun List<Video>.sortVideos(): List<Video> = sort()

    // extensions-lib 14's name for sortVideos
    protected open fun List<Video>.sort(): List<Video> = this

    /** Resolves a lib 14 video whose [Video.videoUrl] is `"null"`. */
    open suspend fun getVideoUrl(video: Video): String =
        client.newCall(videoUrlRequest(video)).awaitSuccess().let(::videoUrlParse)

    protected open fun videoUrlRequest(video: Video): Request = GET(video.url, headers)

    protected open fun videoUrlParse(response: Response): String = throw UnsupportedOperationException()

    fun SEpisode.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    fun SAnime.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig)
            var out = uri.path
            if (uri.query != null) out += "?" + uri.query
            if (uri.fragment != null) out += "#" + uri.fragment
            out
        } catch (_: URISyntaxException) {
            orig
        }
    }

    open fun getAnimeUrl(anime: SAnime): String = animeDetailsRequest(anime).url.toString()

    open fun getEpisodeUrl(episode: SEpisode): String = episode.url

    open fun prepareNewEpisode(episode: SEpisode, anime: SAnime) {}
}
