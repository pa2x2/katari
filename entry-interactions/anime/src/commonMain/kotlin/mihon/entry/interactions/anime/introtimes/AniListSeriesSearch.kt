package mihon.entry.interactions.anime.introtimes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.time.Duration.Companion.minutes

/**
 * An AniList anime that AniSkip can have times for. [titles] are every name AniList knows it by, to match a source's
 * title against; [title] is the one shown.
 */
internal data class AniListSeries(
    val anilistId: Long,
    val malId: Long,
    val title: String,
    val titles: List<String>,
    val year: Int?,
    val format: String?,
    val episodes: Int?,
    val coverUrl: String?,
)

/** Finds anime on AniList by title, without signing in. */
@Inject
@SingleIn(AppScope::class)
internal class AniListSeriesSearch(networkHelper: NetworkHelper) {

    // AniList allows 30 requests a minute while it runs degraded and answers 429 above that.
    private val client by lazy { networkHelper.client.newBuilder().rateLimit(permits = 25, period = 1.minutes).build() }

    /** Series without a MyAnimeList id are left out: AniSkip can't have times for them. */
    suspend fun search(query: String): List<AniListSeries> {
        val body = buildJsonObject {
            put("query", SEARCH_QUERY)
            put("variables", buildJsonObject { put("search", query) })
        }.toString().toRequestBody(jsonMime)
        val text = client.newCall(POST(ENDPOINT, body = body)).await().use { response ->
            if (!response.isSuccessful) throw HttpException(response.code)
            withContext(Dispatchers.IO) { response.body.string() }
        }
        return json.decodeFromString<SearchResponse>(text).data.page.media.mapNotNull { it.toSeries() }
    }

    private fun Media.toSeries(): AniListSeries? {
        val malId = idMal ?: return null
        val shown = title.romaji ?: title.english ?: title.native ?: return null
        return AniListSeries(
            anilistId = id,
            malId = malId,
            title = shown,
            titles = listOfNotNull(title.romaji, title.english, title.native) + synonyms.orEmpty(),
            year = seasonYear ?: startDate?.year,
            format = format,
            episodes = episodes,
            coverUrl = coverImage?.medium,
        )
    }

    @Serializable
    private data class SearchResponse(val data: Data)

    @Serializable
    private data class Data(@SerialName("Page") val page: Page)

    @Serializable
    private data class Page(val media: List<Media>)

    @Serializable
    private data class Media(
        val id: Long,
        val idMal: Long? = null,
        val title: Title,
        val synonyms: List<String>? = null,
        val seasonYear: Int? = null,
        val startDate: FuzzyDate? = null,
        val format: String? = null,
        val episodes: Int? = null,
        val coverImage: CoverImage? = null,
    )

    @Serializable
    private data class Title(val romaji: String? = null, val english: String? = null, val native: String? = null)

    @Serializable
    private data class FuzzyDate(val year: Int? = null)

    @Serializable
    private data class CoverImage(val medium: String? = null)

    private companion object {
        const val ENDPOINT = "https://graphql.anilist.co"
        const val SEARCH_QUERY = "query(\$search: String) { Page(perPage: 10) { media(search: \$search, type: ANIME) " +
            "{ id idMal title { romaji english native } synonyms seasonYear startDate { year } format episodes " +
            "coverImage { medium } } } }"
        val json = Json { ignoreUnknownKeys = true }
    }
}
