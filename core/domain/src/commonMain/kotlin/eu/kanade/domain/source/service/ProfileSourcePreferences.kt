package eu.kanade.domain.source.service

import app.katari.profiles.core.ProfileStore
import kotlinx.serialization.json.Json

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
