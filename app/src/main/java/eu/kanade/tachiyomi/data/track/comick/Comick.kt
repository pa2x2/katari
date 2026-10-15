package eu.kanade.tachiyomi.data.track.comick

import android.net.Uri
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.DeletableTracker
import eu.kanade.tachiyomi.data.track.ExternalLoginTracker
import eu.kanade.tachiyomi.data.track.TrackerDependencies
import eu.kanade.tachiyomi.data.track.comick.dto.ComickOAuth
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.PkceUtil
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.i18n.*
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Clock
import tachiyomi.domain.track.model.EntryTrack as DomainTrack

class Comick(
    id: Long,
    dependencies: TrackerDependencies,
    private val profileStore: ProfileStore? = null,
) : BaseTracker(id, "Comick", dependencies), DeletableTracker, ExternalLoginTracker {

    override val accountOrder = 8

    private val interceptor by lazy { ComickInterceptor(this, json) }
    private val api by lazy { ComickApi(id, client, interceptor, json) }

    override fun getLogo() = R.drawable.brand_comick

    override fun getStatusList(): List<Long> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

    override fun getStatus(status: Long): StringResource? = when (status) {
        READING -> MR.strings.reading
        COMPLETED -> MR.strings.completed
        ON_HOLD -> MR.strings.on_hold
        DROPPED -> MR.strings.dropped
        PLAN_TO_READ -> MR.strings.plan_to_read
        else -> null
    }

    override fun getReadingStatus(): Long = READING

    override fun getRereadingStatus(): Long = -1L

    override fun getCompletionStatus(): Long = COMPLETED

    override fun getScoreList(): List<String> = IntRange(0, 10).map(Int::toString).toList()

    override fun displayScore(track: DomainTrack): String = track.score.toInt().toString()

    override suspend fun update(
        track: Track,
        didReadChapter: Boolean,
    ): Track {
        if (track.status != COMPLETED && track.status != READING && didReadChapter) {
            track.status = READING
            if (track.last_chapter_read == 1.0) {
                track.started_reading_date = Clock.System.now().toEpochMilliseconds()
            }
        }

        return api.updateLibManga(track)
    }

    override suspend fun bind(
        track: Track,
        hasReadChapters: Boolean,
    ): Track {
        val remoteTrack = api.findLibManga(track)
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)

            update(track)
        } else {
            track.status = if (hasReadChapters) READING else PLAN_TO_READ
            track.score = 0.0

            api.addLibManga(track)
        }
    }

    override suspend fun search(query: String): List<TrackSearch> {
        if (query.startsWith(SEARCH_ID_PREFIX)) {
            query.substringAfter(SEARCH_ID_PREFIX).trim().let { hid ->
                return api.getMangaDetails(hid)?.let { listOf(it) } ?: emptyList()
            }
        }

        return api.search(query)
    }

    override suspend fun refresh(track: Track): Track {
        val remoteTrack = api.findLibManga(track) ?: throw Exception("Could not find manga")
        track.copyPersonalFrom(remoteTrack)
        return track
    }

    override suspend fun login(username: String, password: String) {
        throw UnsupportedOperationException("Comick requires OAuth")
    }

    /**
     * Finishes the authorization started by [authorizationUri] for the profile encoded in [state].
     *
     * @throws ComickMissingScopesException when the user did not grant the scopes tracking needs; the login activity
     * shows its message.
     */
    suspend fun completeOAuth(code: String, state: String): Boolean {
        val profileId = state.substringBefore('.').toLongOrNull() ?: return false
        val preferences = preferences(profileId)
        if (preferences.oauthState(this).get() != state) return false

        return try {
            val codeVerifier = preferences.oauthCodeVerifier(this).get()
            val oauth = api.getAccessToken(code, codeVerifier)
            val currentUser = api.getCurrentUser(oauth.accessToken)
            preferences.trackDisplayUsername(this).set(currentUser.username ?: currentUser.id)
            preferences.trackToken(this).set(json.encodeToString(oauth))
            preferences.setCredentials(this, "user", oauth.accessToken)
            true
        } catch (e: Exception) {
            logout(profileId)
            if (e is ComickMissingScopesException) throw e
            false
        } finally {
            preferences.oauthState(this).delete()
            preferences.oauthCodeVerifier(this).delete()
        }
    }

    override fun authorizationUri(): Uri {
        val profileId = currentProfileId()
        val preferences = preferences(profileId)
        val codes = PkceUtil.generateS256Codes()
        val random = ByteArray(16).also(SecureRandom()::nextBytes)
        val state = "$profileId.${Base64.getUrlEncoder().withoutPadding().encodeToString(random)}"
        preferences.oauthState(this).set(state)
        preferences.oauthCodeVerifier(this).set(codes.codeVerifier)
        return ComickApi.authUrl(codes.codeChallenge, state)
    }

    fun cancelOAuth(state: String): Boolean {
        val profileId = state.substringBefore('.').toLongOrNull() ?: return false
        val preferences = preferences(profileId)
        if (preferences.oauthState(this).get() != state) return false
        preferences.oauthState(this).delete()
        preferences.oauthCodeVerifier(this).delete()
        return true
    }

    override suspend fun updateUserConfig() {
        val currentUser = api.getCurrentUser()
        saveDisplayUsername(currentUser.username ?: currentUser.id)
    }

    fun saveToken(oauth: ComickOAuth?, profileId: Long = currentProfileId()) {
        val preference = preferences(profileId).trackToken(this)
        if (oauth == null) preference.delete() else preference.set(json.encodeToString(oauth))
    }

    fun restoreToken(profileId: Long = currentProfileId()): ComickOAuth? {
        return try {
            json.decodeFromString(preferences(profileId).trackToken(this).get())
        } catch (_: Exception) {
            null
        }
    }

    override fun logout() {
        super.logout()
        trackPreferences.trackToken(this).delete()
        trackPreferences.oauthState(this).delete()
        trackPreferences.oauthCodeVerifier(this).delete()
    }

    private fun logout(profileId: Long) {
        val preferences = preferences(profileId)
        preferences.setCredentials(this, "", "")
        preferences.trackDisplayUsername(this).delete()
        preferences.trackToken(this).delete()
        preferences.oauthState(this).delete()
        preferences.oauthCodeVerifier(this).delete()
    }

    internal fun currentProfileId(): Long = profileStore?.currentProfileId ?: 0L

    private fun preferences(profileId: Long): TrackPreferences {
        return profileStore?.let { TrackPreferences(it.privateStore(profileId)) } ?: trackPreferences
    }

    override suspend fun delete(track: DomainTrack) {
        api.deleteLibManga(track)
    }

    companion object {
        const val READING = 1L
        const val COMPLETED = 2L
        const val ON_HOLD = 3L
        const val DROPPED = 4L
        const val PLAN_TO_READ = 5L

        private const val SEARCH_ID_PREFIX = "id:"
    }
}
