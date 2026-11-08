package eu.kanade.tachiyomi.ui.video.player

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

internal class AnimePlayerBasePreferences(
    preferenceStore: PreferenceStore,
) {
    val incognitoMode = preferenceStore.getBoolean(Preference.appStateKey("incognito_mode"), false)

    // Keep this session-scoped so every new app process starts immersive playback muted.
    var immersiveFeedMuted = true
}
