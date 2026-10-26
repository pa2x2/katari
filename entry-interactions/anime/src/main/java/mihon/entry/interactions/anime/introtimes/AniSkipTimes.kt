package mihon.entry.interactions.anime.introtimes

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.math.roundToLong

/** Asks AniSkip for the intro, credits and recap times viewers submitted for an episode. */
@Inject
@SingleIn(AppScope::class)
internal class AniSkipTimes(private val networkHelper: NetworkHelper) {

    /**
     * The times submitted for [episode] of the MyAnimeList series [malId] on a cut [lengthSeconds] long; AniSkip leaves
     * out those of other cuts, whose times would be off. Empty when it has none.
     */
    suspend fun fetch(malId: Long, episode: Int, lengthSeconds: Long): List<PlaybackMarker> {
        val url = "$ENDPOINT/$malId/$episode".toHttpUrl().newBuilder()
            .apply { SkipTypes.keys.forEach { addQueryParameter("types[]", it) } }
            .addQueryParameter("episodeLength", lengthSeconds.toString())
            .build()
        return networkHelper.client.newCall(GET(url)).await().use { response ->
            when {
                // AniSkip answers an episode it has nothing for with 404.
                response.code == HTTP_NOT_FOUND -> emptyList()
                !response.isSuccessful -> throw HttpException(response.code)
                else -> aniSkipMarkers(withContext(Dispatchers.IO) { response.body.string() })
            }
        }
    }

    private companion object {
        const val ENDPOINT = "https://api.aniskip.com/v2/skip-times"
        const val HTTP_NOT_FOUND = 404
    }
}

/** The markers in an AniSkip answer; a skip type Katari doesn't know is left out rather than failing the rest. */
internal fun aniSkipMarkers(body: String): List<PlaybackMarker> =
    json.decodeFromString<SkipTimesResponse>(body).results.orEmpty().mapNotNull { result ->
        val kind = SkipTypes[result.skipType] ?: return@mapNotNull null
        val startMs = (result.interval.startTime * MS_PER_SECOND).roundToLong()
        val endMs = (result.interval.endTime * MS_PER_SECOND).roundToLong()
        PlaybackMarker(startMs = startMs, endMs = endMs, kind = kind).takeIf { endMs > startMs }
    }

private val SkipTypes = mapOf(
    "op" to PlaybackMarkerKind.INTRO,
    "mixed-op" to PlaybackMarkerKind.MIXED_INTRO,
    "ed" to PlaybackMarkerKind.OUTRO,
    "mixed-ed" to PlaybackMarkerKind.MIXED_OUTRO,
    "recap" to PlaybackMarkerKind.RECAP,
)

@Serializable
private data class SkipTimesResponse(val results: List<SkipTime>? = null)

@Serializable
private data class SkipTime(val interval: Interval, val skipType: String)

@Serializable
private data class Interval(val startTime: Double, val endTime: Double)

private val json = Json { ignoreUnknownKeys = true }

private const val MS_PER_SECOND = 1000.0
