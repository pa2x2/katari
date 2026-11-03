package eu.kanade.tachiyomi.data.track.kavita

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.EntryTrackingSource
import eu.kanade.tachiyomi.data.track.TrackerDependencies
import eu.kanade.tachiyomi.data.track.TrackerSourceKeys
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.source.TrackerSourceAccess
import mihon.trackers.resources.Res
import mihon.trackers.resources.brand_kavita
import org.jetbrains.compose.resources.DrawableResource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.track.model.EntryTrack
import tachiyomi.i18n.*

class Kavita(
    id: Long,
    dependencies: TrackerDependencies,
    private val sourceKeys: TrackerSourceKeys,
    private val sourceAccess: Lazy<TrackerSourceAccess>,
) : BaseTracker(id, "Kavita", dependencies), EnhancedTracker {

    companion object {
        const val UNREAD = 1L
        const val READING = 2L
        const val COMPLETED = 3L
    }

    var authentications: OAuth? = null

    private val interceptor by lazy { KavitaInterceptor(this, appIdentity) }
    val api by lazy { KavitaApi(id, client, interceptor, json) }

    override fun getLogo(): DrawableResource = Res.drawable.brand_kavita

    override fun getStatusList(): List<Long> = listOf(UNREAD, READING, COMPLETED)

    override fun getStatus(status: Long): StringResource? = when (status) {
        UNREAD -> MR.strings.unread
        READING -> MR.strings.reading
        COMPLETED -> MR.strings.completed
        else -> null
    }

    override fun getReadingStatus(): Long = READING

    override fun getRereadingStatus(): Long = -1

    override fun getCompletionStatus(): Long = COMPLETED

    override fun getScoreList(): List<String> = listOf()

    override fun displayScore(track: EntryTrack): String = ""

    override suspend fun update(track: Track, didReadChapter: Boolean): Track {
        if (track.status != COMPLETED) {
            if (didReadChapter) {
                if (track.last_chapter_read.toLong() == track.total_chapters && track.total_chapters > 0) {
                    track.status = COMPLETED
                } else {
                    track.status = READING
                }
            }
        }
        return api.updateProgress(track)
    }

    override suspend fun bind(track: Track, hasReadChapters: Boolean): Track {
        return track
    }

    override suspend fun search(query: String): List<TrackSearch> {
        TODO("Not yet implemented: search")
    }

    override suspend fun refresh(track: Track): Track {
        val remoteTrack = api.getTrackSearch(track.tracking_url)
        track.copyPersonalFrom(remoteTrack)
        track.total_chapters = remoteTrack.total_chapters
        return track
    }

    override suspend fun login(username: String, password: String) {
        saveCredentials("user", "pass")
    }

    // [Tracker].isLogged works by checking that credentials are saved.
    // By saving dummy, unused credentials, we can activate the tracker simply by login/logout
    override fun loginNoop() {
        saveCredentials("user", "pass")
    }

    override fun getAcceptedSources() = listOf("eu.kanade.tachiyomi.extension.all.kavita.Kavita")

    override suspend fun match(entry: Entry): TrackSearch? =
        try {
            api.getTrackSearch(entry.url)
        } catch (_: Exception) {
            null
        }

    override fun isTrackFrom(track: EntryTrack, entry: Entry, source: EntryTrackingSource?): Boolean =
        track.remoteUrl == entry.url && source?.let { accept(it) } == true

    override fun migrateTrack(track: EntryTrack, entry: Entry, newSource: EntryTrackingSource): EntryTrack? =
        if (accept(newSource)) {
            track.copy(remoteUrl = entry.url)
        } else {
            null
        }

    suspend fun loadOAuth() {
        val oauth = OAuth()
        for (id in 1..3) {
            val authentication = oauth.authentications[id - 1]
            val sourceKey = sourceKeys.keyOf(name = "kavita_$id", lang = "all", versionId = 1) ?: continue
            val preferences = sourceAccess.value.settings(sourceKey) ?: continue

            val prefApiUrl = preferences.getString("APIURL", "")
            val prefApiKey = preferences.getString("APIKEY", "")
            if (prefApiUrl.isNullOrEmpty() || prefApiKey.isNullOrEmpty()) {
                // Source not configured. Skip
                continue
            }

            val token = api.getNewToken(apiUrl = prefApiUrl, apiKey = prefApiKey)
            if (token.isNullOrEmpty()) {
                // Source is not accessible. Skip
                continue
            }

            authentication.apiUrl = prefApiUrl
            authentication.jwtToken = token
        }
        authentications = oauth
    }

    override suspend fun updateUserConfig() = Unit
}
