package app.katari.profiles.core

import app.katari.core.common.CustomPreferences
import app.katari.core.common.HomeScreenTabs
import app.katari.core.common.defaultHomeScreenTabs
import app.katari.core.metro.AppCoroutineScope
import app.katari.domain.profile.ProfileSeedPreferences
import app.katari.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Inject
@SingleIn(AppScope::class)
class ProfileManager(
    private val profileDatabase: ProfileDatabase,
    private val profileStore: ProfileStore,
    private val profilesPreferences: ProfilesPreferences,
    private val legacyPreferenceKeys: LegacyProfilePreferenceKeys,
    private val extensions: ProfileExtensions,
    private val deletion: ProfileDeletion,
    private val bookReaderFonts: BookDocumentReaderFontLibrary,
    private val libraryUpdateQueue: LibraryUpdateQueue,
    private val libraryUpdateSchedule: LibraryUpdateSchedule,
    private val statisticsRecapSchedule: StatisticsRecapSchedule,
    @AppCoroutineScope appScope: CoroutineScope,
) {
    private val scope = appScope + Dispatchers.Default
    private val switchRequests = MutableStateFlow(profilesPreferences.activeProfileId.get())
    private val lockGate = ProfileLockGate(profileStore)

    val activeProfileId: Long
        get() = profileStore.currentProfileId

    val profiles: StateFlow<List<Profile>> = profileDatabase.subscribeProfiles(includeArchived = true)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val visibleProfiles: StateFlow<List<Profile>> = profiles
        .map { profiles -> profiles.filterNot(Profile::isArchived) }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val activeProfile: StateFlow<Profile?> = combine(profiles, switchRequests) { profiles, activeId ->
        profiles.firstOrNull { it.id == activeId }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun ensureDefaultProfile() {
        val defaultProfile = profileDatabase.getProfileById(ProfileConstants.DEFAULT_PROFILE_ID)
        if (defaultProfile == null) {
            profileDatabase.insertProfile(
                uuid = ProfileConstants.DEFAULT_PROFILE_UUID,
                name = ProfileConstants.DEFAULT_PROFILE_NAME,
                colorSeed = 0x4F6AF2,
                position = 0,
                requiresAuth = false,
                isArchived = false,
            )
        }

        migrateLegacyPreferencesIfNeeded()
    }

    suspend fun loadStartupSnapshot(): ProfileStartupSnapshot {
        ensureDefaultProfile()
        val loadedProfiles = profileDatabase.getProfiles(includeArchived = true)
        val visibleProfiles = loadedProfiles.filterNot(Profile::isArchived)
        val activeId = profilesPreferences.activeProfileId.get()
        // A profile archived while it was open, on another device or by a restored backup, isn't opened again
        val initialProfile = visibleProfiles.firstOrNull { it.id == activeId }
            ?: visibleProfiles.firstOrNull()
            ?: loadedProfiles.firstOrNull { it.id == ProfileConstants.DEFAULT_PROFILE_ID }
        if (initialProfile != null) {
            activateProfile(initialProfile)
        }
        return ProfileStartupSnapshot(
            initialProfile = initialProfile,
            visibleProfiles = visibleProfiles,
            shouldShowPicker = profilesPreferences.pickerEnabled.get() && visibleProfiles.size > 1,
        )
    }

    suspend fun setActiveProfile(profileId: Long, rescheduleJobs: Boolean = true) {
        val profile = profileDatabase.getProfileById(profileId) ?: return
        activateProfile(profile)
        if (rescheduleJobs) {
            statisticsRecapSchedule.setupTask()
        }
    }

    private fun activateProfile(profile: Profile) {
        val previousProfileId = profileStore.currentProfileId
        if (previousProfileId != profile.id) {
            lockGate.markInactive(previousProfileId)
        }
        profileStore.setCurrentProfileId(profile.id)
        switchRequests.value = profile.id
    }

    suspend fun createProfile(name: String): Profile {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty())
        validateUniqueProfileName(trimmedName)

        val position = profiles.value.maxOfOrNull(Profile::position)?.plus(1) ?: 1L
        val id = profileDatabase.insertProfile(
            uuid = newUuid(),
            name = trimmedName,
            colorSeed = Random.nextLong(0x00FFFFFF),
            position = position,
            requiresAuth = false,
            isArchived = false,
        )
        profileStore.deleteProfileState(id)
        extensions.deleteSettings(id)
        bookReaderFonts.deleteProfile(id)
        val hiddenSourceIds = extensions.installedSourceIds().mapTo(mutableSetOf(), Long::toString)
        profileStore.profileStore(id)
            .getStringSet(SourcePreferences.HIDDEN_SOURCES_KEY, hiddenSourceIds)
            .set(hiddenSourceIds)
        val customPreferences = CustomPreferences(profileStore.profileStore(id))
        customPreferences.homeScreenTabs.set(defaultHomeScreenTabs())
        customPreferences.homeScreenStartupTab.set(HomeScreenTabs.Library)
        seedPreferences(profileId = id)
        libraryUpdateSchedule.schedule(id)
        return requireNotNull(profileDatabase.getProfileById(id))
    }

    suspend fun renameProfile(profileId: Long, name: String) {
        val profile = profileDatabase.getProfileById(profileId) ?: return
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty())
        validateUniqueProfileName(trimmedName, excludedProfileId = profileId)
        profileDatabase.updateProfile(id = profileId, name = trimmedName)
    }

    suspend fun setProfileArchived(profileId: Long, archived: Boolean) {
        if (profileId == ProfileConstants.DEFAULT_PROFILE_ID && archived) return
        if (archived && activeProfileId == profileId) return
        profileDatabase.updateProfile(id = profileId, isArchived = archived)
        // An archived profile's library isn't updated until the profile comes back.
        if (archived) libraryUpdateQueue.stop(profileId)
        libraryUpdateSchedule.schedule(profileId)
    }

    suspend fun permanentlyDeleteProfile(profileId: Long) {
        if (profileId == ProfileConstants.DEFAULT_PROFILE_ID) return
        // A restored backup can archive the active profile; switching away from it here would open the next profile
        // without its unlock, so it is deleted only once another profile is open.
        if (profileId == activeProfileId) return
        val profile = profileDatabase.getProfileById(profileId) ?: return
        if (!profile.isArchived) return
        deletion.delete(profileId)
    }

    /** Whether this profile is protected by biometrics at all, regardless of its current lock state. */
    fun profileRequiresUnlock(profileId: Long): Boolean {
        return SecurityPreferences(profileStore.profileStore(profileId)).useAuthenticator.get()
    }

    /**
     * Whether entering [profileId] must authenticate now, honoring the profile's "Lock when idle" delay.
     *
     * Unlike [profileRequiresUnlock], this reflects the current lock state: a protected profile unlocked earlier in
     * this process and left within its idle delay is entered without prompting.
     */
    fun profileRequiresAuthNow(profileId: Long): Boolean = lockGate.requiresAuthNow(profileId)

    /** Records that [profileId] proved presence through authentication during this process. */
    fun markProfileAuthenticated(profileId: Long) = lockGate.markAuthenticated(profileId)

    private suspend fun migrateLegacyPreferencesIfNeeded() {
        val currentVersion = profilesPreferences.legacyPreferenceMigrationVersion.get()
        if (currentVersion >= LEGACY_PROFILE_MIGRATION_VERSION) return

        legacyPreferenceKeys.migrate(fromVersion = currentVersion) {
            migrateLegacyProfileUnlockSettings()
            migrateLegacySourceListPreferences()
            seedPreferencesOf(
                profileDatabase.getProfiles(includeArchived = true)
                    .map(Profile::id)
                    .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) },
            )
        }
        profilesPreferences.legacyPreferenceMigrationVersion.set(LEGACY_PROFILE_MIGRATION_VERSION)
    }

    private suspend fun migrateLegacyProfileUnlockSettings() {
        profileDatabase.getProfiles(includeArchived = true).forEach { profile ->
            if (profile.requiresAuth) {
                SecurityPreferences(profileStore.profileStore(profile.id)).useAuthenticator.set(true)
                profileDatabase.updateProfile(id = profile.id, requiresAuth = false)
            }
        }
    }

    private fun seedPreferencesOf(profileIds: Iterable<Long>) {
        profileIds.forEach { seedPreferences(it) }
    }

    private fun seedPreferences(profileId: Long) {
        ProfileSeedPreferences.seed(profileStore.profileStore(profileId))
    }

    private fun migrateLegacySourceListPreferences() {
        val profiles = runCatching {
            kotlinx.coroutines.runBlocking { profileDatabase.getProfiles(includeArchived = true) }
        }.getOrDefault(emptyList())

        profiles.forEach { profile ->
            val store = profileStore.profileStore(profile.id)
            val hidden = store.getStringSet(SourcePreferences.HIDDEN_SOURCES_KEY, emptySet())
            val legacy = store.getStringSet(SourcePreferences.LEGACY_HIDDEN_SOURCES_KEY, emptySet())
            val manga = store.getStringSet(SourcePreferences.MANGA_HIDDEN_SOURCES_KEY, emptySet())
            val anime = store.getStringSet(SourcePreferences.ANIME_HIDDEN_SOURCES_KEY, emptySet())
            val pinned = store.getStringSet(SourcePreferences.PINNED_SOURCES_KEY, emptySet())
            val pinnedManga = store.getStringSet(SourcePreferences.MANGA_PINNED_SOURCES_KEY, emptySet())
            val pinnedAnime = store.getStringSet(SourcePreferences.ANIME_PINNED_SOURCES_KEY, emptySet())
            val lastUsed = store.getLong(Preference.appStateKey(SourcePreferences.LAST_USED_SOURCE_KEY), -1)
            val lastUsedManga = store.getLong(Preference.appStateKey(SourcePreferences.MANGA_LAST_USED_SOURCE_KEY), -1)
            val lastUsedAnime = store.getLong(Preference.appStateKey(SourcePreferences.ANIME_LAST_USED_SOURCE_KEY), -1)

            val hiddenSources = buildSet {
                if (hidden.isSet()) addAll(hidden.get())
                if (legacy.isSet()) addAll(legacy.get())
                if (manga.isSet()) addAll(manga.get())
                if (anime.isSet()) addAll(anime.get())
            }
            if (hiddenSources.isNotEmpty() || hidden.isSet() || legacy.isSet() || manga.isSet() || anime.isSet()) {
                hidden.set(hiddenSources)
            }

            val pinnedSources = buildSet {
                if (pinned.isSet()) addAll(pinned.get())
                if (pinnedManga.isSet()) addAll(pinnedManga.get())
                if (pinnedAnime.isSet()) addAll(pinnedAnime.get())
            }
            if (pinnedSources.isNotEmpty() || pinned.isSet() || pinnedManga.isSet() || pinnedAnime.isSet()) {
                pinned.set(pinnedSources)
            }

            if (!lastUsed.isSet()) {
                val migratedLastUsed = when {
                    lastUsedManga.isSet() && lastUsedManga.get() != -1L -> lastUsedManga.get()
                    lastUsedAnime.isSet() && lastUsedAnime.get() != -1L -> lastUsedAnime.get()
                    else -> null
                }
                if (migratedLastUsed != null) {
                    lastUsed.set(migratedLastUsed)
                }
            }

            if (legacy.isSet()) {
                legacy.delete()
            }
            if (manga.isSet()) {
                manga.delete()
            }
            if (anime.isSet()) {
                anime.delete()
            }
            if (pinnedManga.isSet()) {
                pinnedManga.delete()
            }
            if (pinnedAnime.isSet()) {
                pinnedAnime.delete()
            }
            if (lastUsedManga.isSet()) {
                lastUsedManga.delete()
            }
            if (lastUsedAnime.isSet()) {
                lastUsedAnime.delete()
            }
        }
    }

    suspend fun getProfileBundles(includeArchived: Boolean = true): List<ProfileBundle> {
        return profileDatabase.getProfiles(includeArchived).map { profile ->
            ProfileBundle(
                profile = profile.copy(requiresAuth = profileRequiresUnlock(profile.id)),
                categories = profileDatabase.getAllCategories(profile.id).map { it.id },
                mangaCount = profileDatabase.getMangaCount(profile.id).toInt(),
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newUuid(): String {
        return Uuid.random().toString()
    }

    private suspend fun validateUniqueProfileName(
        name: String,
        excludedProfileId: Long? = null,
    ) {
        require(
            !profileDatabase.getProfiles(includeArchived = true)
                .hasNameConflict(name = name, excludedProfileId = excludedProfileId),
        ) {
            "Profile name already exists"
        }
    }

    companion object {
        private const val LEGACY_PROFILE_MIGRATION_VERSION = 12
    }
}
