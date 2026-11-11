package app.katari.entry.interactions.anime.introtimes.aniskip

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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.time.Duration.Companion.minutes

/**
 * An AniList anime that AniSkip can have times for. [titles] are every name AniList knows it by, to match a source's
 * title against; [title] is the one shown.
 *
 * @property startOrder its start date as one number to order seasons by; 0 when AniList has none.
 * @property episodes how many episodes it has; null while it airs or when AniList doesn't say.
 * @property airedEpisodes how many have aired so far, while it airs.
 * @property episodeNames the names AniList has for its episodes, by number; it has them for few series.
 * @property prequels the anime it continues, and [sequels] those that continue it.
 */
internal data class AniListSeries(
    val anilistId: Long,
    val malId: Long,
    val title: String,
    val titles: List<String>,
    val year: Int?,
    val startOrder: Int,
    val format: String?,
    val episodes: Int?,
    val airedEpisodes: Int?,
    val coverUrl: String?,
    val episodeNames: Map<Int, String>,
    val prequels: List<AniListLink>,
    val sequels: List<AniListLink>,
) {
    /** A run of episodes, which a season is, rather than a film, a special or an OVA. */
    val isSeries: Boolean get() = format in SeriesFormats
}

/** Another anime an [AniListSeries] is linked to. */
internal data class AniListLink(val anilistId: Long, val isSeries: Boolean)

/** Finds anime on AniList by title or id, without signing in. */
@Inject
@SingleIn(AppScope::class)
internal class AniListSeriesSearch(networkHelper: NetworkHelper) {

    // AniList allows 30 requests a minute while it runs degraded and answers 429 above that.
    private val client by lazy { networkHelper.client.newBuilder().rateLimit(permits = 25, period = 1.minutes).build() }

    /** In AniList's order of relevance. Series without a MyAnimeList id are left out: AniSkip can't have times for them. */
    suspend fun search(query: String): List<AniListSeries> =
        ask("search: \$search", "\$search: String", buildJsonObject { put("search", query) })

    suspend fun byIds(anilistIds: List<Long>): List<AniListSeries> = ask(
        "id_in: \$ids",
        "\$ids: [Int]",
        buildJsonObject { put("ids", buildJsonArray { anilistIds.forEach { add(JsonPrimitive(it)) } }) },
    )

    private suspend fun ask(filter: String, parameter: String, variables: JsonObject): List<AniListSeries> {
        val body = buildJsonObject {
            put("query", "query($parameter) { Page(perPage: $PAGE_SIZE) { media($filter, type: ANIME) { $FIELDS } } }")
            put("variables", variables)
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
        val links = relations?.edges.orEmpty().filter { it.node.type == ANIME }
        return AniListSeries(
            anilistId = id,
            malId = malId,
            title = shown,
            titles = listOfNotNull(title.romaji, title.english, title.native) + synonyms.orEmpty(),
            year = seasonYear ?: startDate?.year,
            startOrder = startDate?.let { (it.year ?: 0) * 10_000 + (it.month ?: 0) * 100 + (it.day ?: 0) } ?: 0,
            format = format,
            episodes = episodes,
            airedEpisodes = nextAiringEpisode?.let { it.episode - 1 }?.takeIf { episodes == null && it > 0 },
            coverUrl = coverImage?.medium,
            episodeNames = episodeNames(streamingEpisodes.orEmpty().mapNotNull { it.title }),
            prequels = links.filter { it.relationType == "PREQUEL" }.map { it.node.toLink() },
            sequels = links.filter { it.relationType == "SEQUEL" }.map { it.node.toLink() },
        )
    }

    private fun RelatedMedia.toLink() = AniListLink(anilistId = id, isSeries = format in SeriesFormats)

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
        val nextAiringEpisode: AiringEpisode? = null,
        val coverImage: CoverImage? = null,
        val streamingEpisodes: List<StreamingEpisode>? = null,
        val relations: Relations? = null,
    )

    @Serializable
    private data class Title(val romaji: String? = null, val english: String? = null, val native: String? = null)

    @Serializable
    private data class FuzzyDate(val year: Int? = null, val month: Int? = null, val day: Int? = null)

    @Serializable
    private data class AiringEpisode(val episode: Int)

    @Serializable
    private data class CoverImage(val medium: String? = null)

    @Serializable
    private data class StreamingEpisode(val title: String? = null)

    @Serializable
    private data class Relations(val edges: List<Relation> = emptyList())

    @Serializable
    private data class Relation(val relationType: String? = null, val node: RelatedMedia)

    @Serializable
    private data class RelatedMedia(val id: Long, val type: String? = null, val format: String? = null)

    private companion object {
        const val ENDPOINT = "https://graphql.anilist.co"
        const val PAGE_SIZE = 10
        const val ANIME = "ANIME"
        const val FIELDS = "id idMal title { romaji english native } synonyms seasonYear " +
            "startDate { year month day } format episodes nextAiringEpisode { episode } coverImage { medium } " +
            "streamingEpisodes { title } relations { edges { relationType node { id type format } } }"
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** AniList's episode names by number, out of the streaming titles it keeps them in, such as "Episode 3 - Name". */
internal fun episodeNames(streamingTitles: List<String>): Map<Int, String> =
    streamingTitles.mapNotNull { title ->
        StreamingTitle.matchEntire(title.trim())?.let { it.groupValues[1].toInt() to it.groupValues[2] }
    }.toMap()

private val StreamingTitle = Regex("""Episode (\d+)\s*[-–:]\s*(.+)""")

// AniList's formats for anime that run as a series.
private val SeriesFormats = setOf("TV", "TV_SHORT", "ONA")
