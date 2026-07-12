package eu.kanade.domain.track.service

import eu.kanade.tachiyomi.data.track.anilist.Anilist
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class GlobalTrackPreferences(
    preferenceStore: PreferenceStore,
) {
    val anilistScoreType: Preference<String> = preferenceStore.getString("anilist_score_type", Anilist.POINT_10)

    val pendingHikkaOAuthProfileId: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("hikka_oauth_profile_id"),
        NO_PENDING_PROFILE,
    )

    companion object {
        const val NO_PENDING_PROFILE = -1L
    }
}
