package eu.kanade.tachiyomi.data.library.notification

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfileStore

/** What the notifications of one profile's update show, which needn't be the active profile. */
data class LibraryUpdateNotificationProfile(
    val id: Long,
    /** Shown in the notification header; null while the profile is the only visible one. */
    val label: String?,
    /** Titles and covers stay out of the shade: the profile hides notification content, or is behind an unlock. */
    val hidesContent: Boolean,
    /** Entry actions go through the app, which asks for the unlock, instead of running in the background. */
    val requiresUnlock: Boolean,
)

@Inject
class LibraryUpdateNotificationProfiles(
    private val profileDatabase: ProfileDatabase,
    private val profileStore: ProfileStore,
    private val profileManager: ProfileManager,
) {
    suspend fun resolve(profileId: Long): LibraryUpdateNotificationProfile {
        val requiresUnlock = profileManager.profileRequiresUnlock(profileId)
        return LibraryUpdateNotificationProfile(
            id = profileId,
            label = label(profileId),
            hidesContent = requiresUnlock ||
                SecurityPreferences(profileStore.profileStore(profileId)).hideNotificationContent.get(),
            requiresUnlock = requiresUnlock,
        )
    }

    suspend fun name(profileId: Long): String? = profileDatabase.getProfileById(profileId)?.name

    private suspend fun label(profileId: Long): String? {
        val visible = profileDatabase.getProfiles(includeArchived = false)
        if (visible.size < 2) return null
        return visible.firstOrNull { it.id == profileId }?.name
    }
}
