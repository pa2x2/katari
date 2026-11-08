package mihon.entry.interactions.anime.introtimes.theintrodb

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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import mihon.entry.interactions.anime.introtimes.IntroTimesUserAgent
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

/**
 * Finds series and films on Wikidata by any of their names in any language, keeping those it knows a TVDB, TMDB or
 * IMDb id of, which TheIntroDB is asked by. Wikidata needs no account or key.
 */
@Inject
@SingleIn(AppScope::class)
internal class WikidataMediaSearch(
    networkHelper: NetworkHelper,
    private val userAgent: IntroTimesUserAgent,
) {
    // Wikimedia asks clients to keep their requests few; a search is two.
    private val client by lazy {
        networkHelper.client.newBuilder().rateLimit(permits = 10, period = 10.seconds).build()
    }

    suspend fun search(query: String): List<TheIntroDbCandidate> {
        val searchUrl = ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("action", "query")
            .addQueryParameter("list", "search")
            .addQueryParameter("srsearch", "$query haswbstatement:${MEDIA_ID_PROPERTIES.joinToString("|")}")
            .addQueryParameter("srlimit", MAX_RESULTS.toString())
            .addQueryParameter("format", "json")
            .build()
        val ids = json.decodeFromString<WikidataSearchResponse>(get(searchUrl)).query?.search.orEmpty().map { it.title }
        if (ids.isEmpty()) return emptyList()
        val entitiesUrl = ENDPOINT.toHttpUrl().newBuilder()
            .addQueryParameter("action", "wbgetentities")
            .addQueryParameter("ids", ids.joinToString("|"))
            .addQueryParameter("props", "labels|aliases|claims")
            .addQueryParameter("format", "json")
            .build()
        val entities = json.decodeFromString<WikidataEntitiesResponse>(get(entitiesUrl)).entities
        // In the order Wikidata ranks them by relevance.
        return ids.mapNotNull { entities[it]?.toCandidate() }
    }

    private suspend fun get(url: HttpUrl): String =
        client.newCall(GET(url, headersOf("User-Agent", userAgent.value))).await().use { response ->
            if (!response.isSuccessful) throw HttpException(response.code)
            withContext(Dispatchers.IO) { response.body.string() }
        }

    private companion object {
        const val ENDPOINT = "https://www.wikidata.org/w/api.php"
        const val MAX_RESULTS = 5
    }
}

private fun WikidataEntity.toCandidate(): TheIntroDbCandidate? {
    val seriesTvdbId = text(TVDB_SERIES)?.toLongOrNull()
    val seriesTmdbId = text(TMDB_TV)?.toLongOrNull()
    val film = seriesTvdbId == null && seriesTmdbId == null
    val tvdbId = if (film) text(TVDB_MOVIE)?.toLongOrNull() else seriesTvdbId
    val tmdbId = if (film) text(TMDB_MOVIE)?.toLongOrNull() else seriesTmdbId
    val imdbId = text(IMDB)
    if (tvdbId == null && tmdbId == null && imdbId == null) return null
    val names = (labels.values + aliases.values.flatten()).map { it.value }.distinct()
    val shown =
        labels[Locale.getDefault().language]?.value ?: labels[ENGLISH]?.value ?: names.firstOrNull() ?: return null
    return TheIntroDbCandidate(
        tvdbId = tvdbId,
        tmdbId = tmdbId,
        imdbId = imdbId,
        title = shown,
        titles = names,
        year = years(if (film) PUBLISHED else STARTED).minOrNull() ?: years(PUBLISHED).minOrNull(),
        film = film,
        coverUrl = null,
    )
}

private fun WikidataEntity.text(property: String): String? =
    claims[property].orEmpty().firstNotNullOfOrNull { (it.mainsnak.datavalue?.value as? JsonPrimitive)?.contentOrNull }

private fun WikidataEntity.years(property: String): List<Int> = claims[property].orEmpty().mapNotNull { claim ->
    val time = ((claim.mainsnak.datavalue?.value as? JsonObject)?.get("time") as? JsonPrimitive)?.contentOrNull
    time?.let { TimeYear.find(it)?.groupValues?.get(1)?.toInt() }
}

// The Wikidata properties holding a series' or film's ids on the services TheIntroDB is asked by.
private const val TVDB_SERIES = "P4835"
private const val TVDB_MOVIE = "P12196"
private const val TMDB_TV = "P4983"
private const val TMDB_MOVIE = "P4947"
private const val IMDB = "P345"
private val MEDIA_ID_PROPERTIES = listOf(TVDB_SERIES, TVDB_MOVIE, TMDB_TV, TMDB_MOVIE)

private const val STARTED = "P580"
private const val PUBLISHED = "P577"
private const val ENGLISH = "en"

// Wikidata writes times as "+2013-04-07T00:00:00Z".
private val TimeYear = Regex("^[+-](\\d{4})-")

@Serializable
private data class WikidataSearchResponse(val query: WikidataSearchQuery? = null)

@Serializable
private data class WikidataSearchQuery(val search: List<WikidataSearchHit> = emptyList())

@Serializable
private data class WikidataSearchHit(val title: String)

@Serializable
private data class WikidataEntitiesResponse(val entities: Map<String, WikidataEntity> = emptyMap())

@Serializable
private data class WikidataEntity(
    val labels: Map<String, WikidataText> = emptyMap(),
    val aliases: Map<String, List<WikidataText>> = emptyMap(),
    val claims: Map<String, List<WikidataClaim>> = emptyMap(),
)

@Serializable
private data class WikidataText(val value: String)

@Serializable
private data class WikidataClaim(val mainsnak: WikidataSnak)

@Serializable
private data class WikidataSnak(val datavalue: WikidataDataValue? = null)

/** [value] is a string for an id and an object for a time, so it is read by the property it belongs to. */
@Serializable
private data class WikidataDataValue(val value: JsonElement)

private val json = Json { ignoreUnknownKeys = true }
