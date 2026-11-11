package app.katari.entry.interactions.anime.introtimes.theintrodb

import app.katari.entry.interactions.anime.introtimes.IntroTimesUserAgent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.time.Duration.Companion.seconds

/**
 * An episode of a series as it aired, numbered as TVmaze numbers it, which for most series is as TVDB does. [aired] is
 * null when TVmaze has no date.
 */
internal data class AiredEpisode(
    val season: Int,
    val number: Int,
    val name: String,
    val aired: LocalDate?,
)

/**
 * TVmaze's series, which need no account or key: found by title where Wikidata has none, and listed episode by episode
 * so a viewer can tell which one is playing and an episode past a season's end is found in the next. Its data is shared
 * under CC BY-SA, so what it lists is credited to it.
 */
@Inject
@SingleIn(AppScope::class)
internal class TvMazeShows(
    networkHelper: NetworkHelper,
    private val userAgent: IntroTimesUserAgent,
) {
    // TVmaze answers 20 requests every 10 seconds and 429 above that.
    private val client by lazy {
        networkHelper.client.newBuilder().rateLimit(permits = 15, period = 10.seconds).build()
    }

    /** Series by [query] that TheIntroDB can be asked about, which takes a TVDB or IMDb id. */
    suspend fun search(query: String): List<TheIntroDbCandidate> {
        val url = "$ENDPOINT/search/shows".toHttpUrl().newBuilder().addQueryParameter("q", query).build()
        return json.decodeFromString<List<TvMazeSearchHit>>(get(url) ?: return emptyList()).mapNotNull {
            it.show.toCandidate()
        }
    }

    /** The aired episodes of the series known by [tvdbId] or [imdbId]; empty when TVmaze doesn't know it. */
    suspend fun episodes(tvdbId: Long?, imdbId: String?): List<AiredEpisode> {
        val show = show(tvdbId, imdbId) ?: return emptyList()
        val episodes = get("$ENDPOINT/shows/${show.id}/episodes".toHttpUrl()) ?: return emptyList()
        return json.decodeFromString<List<TvMazeEpisode>>(episodes).mapNotNull { it.toAired() }
    }

    /** The cover of the series known by [tvdbId] or [imdbId]; null when TVmaze doesn't know it or has none. */
    suspend fun cover(tvdbId: Long?, imdbId: String?): String? = show(tvdbId, imdbId)?.image?.medium

    private suspend fun show(tvdbId: Long?, imdbId: String?): TvMazeShow? {
        val lookup = "$ENDPOINT/lookup/shows".toHttpUrl().newBuilder().apply {
            when {
                tvdbId != null -> addQueryParameter("thetvdb", tvdbId.toString())
                imdbId != null -> addQueryParameter("imdb", imdbId)
                else -> return null
            }
        }.build()
        return get(lookup)?.let { json.decodeFromString<TvMazeShow>(it) }
    }

    /** The body TVmaze answers [url] with, or null for its 404, which it gives a series it doesn't know. */
    private suspend fun get(url: HttpUrl): String? =
        client.newCall(GET(url, headersOf("User-Agent", userAgent.value))).await().use { response ->
            when {
                response.code == HTTP_NOT_FOUND -> null
                !response.isSuccessful -> throw HttpException(response.code)
                else -> withContext(Dispatchers.IO) { response.body.string() }
            }
        }

    private companion object {
        const val ENDPOINT = "https://api.tvmaze.com"
        const val HTTP_NOT_FOUND = 404
    }
}

private fun TvMazeShow.toCandidate(): TheIntroDbCandidate? {
    if (externals.thetvdb == null && externals.imdb == null) return null
    return TheIntroDbCandidate(
        tvdbId = externals.thetvdb,
        tmdbId = null,
        imdbId = externals.imdb,
        title = name,
        titles = listOf(name),
        year = premiered?.take(YEAR_LENGTH)?.toIntOrNull(),
        film = false,
        coverUrl = image?.medium,
    )
}

// Specials have no number of their own.
private fun TvMazeEpisode.toAired(): AiredEpisode? = number?.let {
    AiredEpisode(season = season, number = it, name = name, aired = airdate.toLocalDateOrNull())
}

private fun String.toLocalDateOrNull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (_: DateTimeParseException) {
    null
}

private const val YEAR_LENGTH = 4

@Serializable
private data class TvMazeSearchHit(val show: TvMazeShow)

@Serializable
private data class TvMazeShow(
    val id: Long,
    val name: String = "",
    val premiered: String? = null,
    val image: TvMazeImage? = null,
    val externals: TvMazeExternals = TvMazeExternals(),
)

@Serializable
private data class TvMazeImage(val medium: String? = null)

@Serializable
private data class TvMazeExternals(val thetvdb: Long? = null, val imdb: String? = null)

/** TVmaze leaves [airdate] empty for an episode it has no date for. */
@Serializable
private data class TvMazeEpisode(
    val season: Int,
    val number: Int? = null,
    val name: String = "",
    val airdate: String = "",
)

private val json = Json { ignoreUnknownKeys = true }
