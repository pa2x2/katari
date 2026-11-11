package app.katari.entry.interactions.anime.introtimes.theintrodb

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import tachiyomi.domain.entry.model.TheIntroDbMedia
import kotlin.time.Duration.Companion.seconds

/** Asks TheIntroDB for the intro, recap, credits and preview times viewers submitted, without an account. */
@Inject
@SingleIn(AppScope::class)
internal class TheIntroDbTimes(networkHelper: NetworkHelper) {

    // TheIntroDB answers anonymous callers 30 times every 10 seconds and 429 above that.
    private val client by lazy {
        networkHelper.client.newBuilder().rateLimit(permits = 25, period = 10.seconds).build()
    }

    /**
     * The times for [episode] of the series [media], or for the film when [episode] is null, on a cut [lengthMs] long;
     * TheIntroDB answers for the release whose length is within 5 minutes of it. Empty when it has none.
     */
    suspend fun fetch(media: TheIntroDbMedia, episode: TheIntroDbEpisode?, lengthMs: Long): List<PlaybackMarker> {
        val url = ENDPOINT.toHttpUrl().newBuilder().apply {
            when {
                media.tvdbId != null -> addQueryParameter("tvdb_id", media.tvdbId.toString())
                media.tmdbId != null -> addQueryParameter("tmdb_id", media.tmdbId.toString())
                else -> addQueryParameter("imdb_id", media.imdbId)
            }
            when (episode) {
                is TheIntroDbEpisode.InSeason -> {
                    addQueryParameter("season", episode.season.toString())
                    addQueryParameter("episode", episode.episode.toString())
                }
                // A running number is given as an episode of season 1 in TVDB's absolute order.
                is TheIntroDbEpisode.Running -> {
                    addQueryParameter("season", "1")
                    addQueryParameter("episode", episode.number.toString())
                    addQueryParameter("mapping", "absolute")
                }
                null -> Unit
            }
            addQueryParameter("duration_ms", lengthMs.toString())
        }.build()
        return client.newCall(GET(url)).await().use { response ->
            when (response.code) {
                // 404: nothing submitted for the episode yet, or TheIntroDB doesn't know it. 400: the series has no
                // such season or episode, or TVDB doesn't number it in the order asked.
                HTTP_NOT_FOUND, HTTP_BAD_REQUEST -> emptyList()
                else -> {
                    if (!response.isSuccessful) throw HttpException(response.code)
                    theIntroDbMarkers(withContext(Dispatchers.IO) { response.body.string() }, lengthMs)
                }
            }
        }
    }

    private companion object {
        const val ENDPOINT = "https://api.theintrodb.org/v4/media"
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_NOT_FOUND = 404
    }
}

/** The markers in a TheIntroDB answer for an episode or film [lengthMs] long. */
internal fun theIntroDbMarkers(body: String, lengthMs: Long): List<PlaybackMarker> {
    val media = json.decodeFromString<MediaResponse>(body)
    return listOf(
        media.intro to PlaybackMarkerKind.INTRO,
        media.recap to PlaybackMarkerKind.RECAP,
        media.credits to PlaybackMarkerKind.OUTRO,
        media.preview to PlaybackMarkerKind.PREVIEW,
    ).flatMap { (segments, kind) -> segments.mapNotNull { it.toMarker(kind, lengthMs) } }
}

private fun Segment.toMarker(kind: PlaybackMarkerKind, lengthMs: Long): PlaybackMarker? {
    // An intro or recap without a start opens the episode; credits or a preview without an end run to its end.
    val start = startMs ?: 0L
    val end = endMs ?: lengthMs
    // TheIntroDB keeps "this episode has none" as a span of no length.
    return PlaybackMarker(startMs = start, endMs = end, kind = kind).takeIf { end > start }
}

@Serializable
private data class MediaResponse(
    val intro: List<Segment> = emptyList(),
    val recap: List<Segment> = emptyList(),
    val credits: List<Segment> = emptyList(),
    val preview: List<Segment> = emptyList(),
)

@Serializable
private data class Segment(
    @SerialName("start_ms") val startMs: Long? = null,
    @SerialName("end_ms") val endMs: Long? = null,
)

private val json = Json { ignoreUnknownKeys = true }
