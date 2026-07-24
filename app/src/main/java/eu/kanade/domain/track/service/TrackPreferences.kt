package eu.kanade.domain.track.service

import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBaka
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import tachiyomi.core.common.preference.getEnum

class TrackPreferences(
    private val preferenceStore: PreferenceStore,
) {

    companion object {
        val USERNAME_KEY_FAMILY = privateFamily("pref_mangasync_username_")
        val DISPLAY_USERNAME_KEY_FAMILY = privateFamily("pref_mangasync_displayname_")
        val PASSWORD_KEY_FAMILY = privateFamily("pref_mangasync_password_")
        val AUTH_EXPIRED_KEY_FAMILY = privateFamily("pref_tracker_auth_expired_")
        val TOKEN_KEY_FAMILY = privateFamily("track_token_")
        val OAUTH_STATE_KEY_FAMILY = privateFamily("track_oauth_state_")
        val OAUTH_CODE_VERIFIER_KEY_FAMILY = privateFamily("track_oauth_code_verifier_")

        val profileKeyPatterns = setOf(
            USERNAME_KEY_FAMILY,
            DISPLAY_USERNAME_KEY_FAMILY,
            PASSWORD_KEY_FAMILY,
            AUTH_EXPIRED_KEY_FAMILY,
            TOKEN_KEY_FAMILY,
            OAUTH_STATE_KEY_FAMILY,
            OAUTH_CODE_VERIFIER_KEY_FAMILY,
        )

        private fun privateFamily(prefix: String): ProfilePreferenceKeyPattern.Prefix {
            return ProfilePreferenceKeyPattern.Prefix(Preference.privateKey(prefix))
        }
    }

    fun trackUsername(tracker: Tracker) = trackUsername(tracker.id)

    fun trackUsername(trackerId: Long) = preferenceStore.getString(
        USERNAME_KEY_FAMILY.key(trackerId),
        "",
    )

    fun trackDisplayUsername(tracker: Tracker) = trackDisplayUsername(tracker.id)

    fun trackDisplayUsername(trackerId: Long) = preferenceStore.getString(
        DISPLAY_USERNAME_KEY_FAMILY.key(trackerId),
        "",
    )

    fun trackPassword(tracker: Tracker) = trackPassword(tracker.id)

    fun trackPassword(trackerId: Long) = preferenceStore.getString(
        PASSWORD_KEY_FAMILY.key(trackerId),
        "",
    )

    fun trackAuthExpired(tracker: Tracker) = trackAuthExpired(tracker.id)

    fun trackAuthExpired(trackerId: Long) = preferenceStore.getBoolean(
        AUTH_EXPIRED_KEY_FAMILY.key(trackerId),
        false,
    )

    fun setCredentials(tracker: Tracker, username: String, password: String) {
        trackUsername(tracker).set(username)
        trackPassword(tracker).set(password)
        trackAuthExpired(tracker).set(false)
    }

    fun trackToken(tracker: Tracker) = trackToken(tracker.id)

    fun trackToken(trackerId: Long) = preferenceStore.getString(TOKEN_KEY_FAMILY.key(trackerId), "")

    fun oauthState(tracker: Tracker) = oauthState(tracker.id)

    fun oauthState(trackerId: Long) = preferenceStore.getString(
        OAUTH_STATE_KEY_FAMILY.key(trackerId),
        "",
    )

    fun oauthCodeVerifier(tracker: Tracker) = oauthCodeVerifier(tracker.id)

    fun oauthCodeVerifier(trackerId: Long) = preferenceStore.getString(
        OAUTH_CODE_VERIFIER_KEY_FAMILY.key(trackerId),
        "",
    )

    val mangabakaScoreType: Preference<String> = preferenceStore.getString("mangabaka_score_type", MangaBaka.STEP_1)

    val autoUpdateTrack: Preference<Boolean> = preferenceStore.getBoolean("pref_auto_update_manga_sync_key", true)

    val autoUpdateTrackOnMarkRead: Preference<AutoTrackState> = preferenceStore.getEnum(
        "pref_auto_update_manga_on_mark_read",
        AutoTrackState.ALWAYS,
    )
}
