package eu.kanade.domain.source.service

import kotlinx.serialization.json.Json
import mihon.feature.profiles.core.ProfileStore

class ProfileSourcePreferences(
    private val profileStore: ProfileStore,
    private val json: Json,
) {
    fun forProfile(profileId: Long): SourcePreferences {
        return SourcePreferences(
            preferenceStore = profileStore.profileStore(profileId),
            json = json,
        )
    }
}
