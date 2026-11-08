package mihon.feature.library.update.planning

import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

/** How often a profile's automatic library updates run, which each platform's schedule follows. */
@Inject
class LibraryUpdateSchedulePeriod(
    private val profileDatabase: ProfileDatabase,
    private val profileStore: ProfileStore,
    private val rulesRepository: LibraryUpdateRulesRepository,
) {
    /**
     * Hours between automatic updates of [profileId], the shortest interval its library or any category asks for;
     * null when nothing is updated automatically, or the profile is archived or gone.
     *
     * @param prefInterval the library interval when its preference is about to change and doesn't hold it yet.
     */
    suspend fun hours(profileId: Long, prefInterval: Int? = null): Int? {
        val profile = profileDatabase.getProfileById(profileId)
        if (profile == null || profile.isArchived) return null
        val preferences = LibraryPreferences(profileStore.profileStore(profileId))
        return LibraryUpdateSettings.schedulePeriodHours(
            intervalHours = prefInterval ?: preferences.autoUpdateInterval.get(),
            categoryRules = rulesRepository.getCategoryRules(profileId).values,
        )
    }
}
