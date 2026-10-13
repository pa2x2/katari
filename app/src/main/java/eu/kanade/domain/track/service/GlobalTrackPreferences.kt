package eu.kanade.domain.track.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class GlobalTrackPreferences(
    preferenceStore: PreferenceStore,
) {
    val pendingHikkaOAuthProfileId: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("hikka_oauth_profile_id"),
        NO_PENDING_PROFILE,
    )

    companion object {
        const val NO_PENDING_PROFILE = -1L
    }
}
