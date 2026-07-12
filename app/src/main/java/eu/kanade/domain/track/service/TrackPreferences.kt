package eu.kanade.domain.track.service

import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBaka
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

class TrackPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun trackUsername(tracker: Tracker) = trackUsername(tracker.id)

    fun trackUsername(trackerId: Long) = preferenceStore.getString(
        Preference.privateKey("pref_mangasync_username_$trackerId"),
        "",
    )

    fun trackDisplayUsername(tracker: Tracker) = trackDisplayUsername(tracker.id)

    fun trackDisplayUsername(trackerId: Long) = preferenceStore.getString(
        Preference.privateKey("pref_mangasync_displayname_$trackerId"),
        "",
    )

    fun trackPassword(tracker: Tracker) = trackPassword(tracker.id)

    fun trackPassword(trackerId: Long) = preferenceStore.getString(
        Preference.privateKey("pref_mangasync_password_$trackerId"),
        "",
    )

    fun trackAuthExpired(tracker: Tracker) = trackAuthExpired(tracker.id)

    fun trackAuthExpired(trackerId: Long) = preferenceStore.getBoolean(
        Preference.privateKey("pref_tracker_auth_expired_$trackerId"),
        false,
    )

    fun setCredentials(tracker: Tracker, username: String, password: String) {
        trackUsername(tracker).set(username)
        trackPassword(tracker).set(password)
        trackAuthExpired(tracker).set(false)
    }

    fun trackToken(tracker: Tracker) = trackToken(tracker.id)

    fun trackToken(trackerId: Long) = preferenceStore.getString(Preference.privateKey("track_token_$trackerId"), "")

    fun oauthState(tracker: Tracker) = oauthState(tracker.id)

    fun oauthState(trackerId: Long) = preferenceStore.getString(
        Preference.privateKey("track_oauth_state_$trackerId"),
        "",
    )

    fun oauthCodeVerifier(tracker: Tracker) = oauthCodeVerifier(tracker.id)

    fun oauthCodeVerifier(trackerId: Long) = preferenceStore.getString(
        Preference.privateKey("track_oauth_code_verifier_$trackerId"),
        "",
    )

    val mangabakaScoreType: Preference<String> = preferenceStore.getString("mangabaka_score_type", MangaBaka.STEP_1)

    val autoUpdateTrack: Preference<Boolean> = preferenceStore.getBoolean("pref_auto_update_manga_sync_key", true)

    val autoUpdateTrackOnMarkRead: Preference<AutoTrackState> = preferenceStore.getEnum(
        "pref_auto_update_manga_on_mark_read",
        AutoTrackState.ALWAYS,
    )
}
