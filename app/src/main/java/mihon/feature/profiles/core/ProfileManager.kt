package mihon.feature.profiles.core

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.extension.ExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import logcat.LogPriority
import mihon.core.common.CustomPreferences
import mihon.core.common.HomeScreenTabs
import mihon.core.common.defaultHomeScreenTabs
import mihon.core.common.toHomeScreenTabPreferenceValue
import mihon.entry.interactions.ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID
import mihon.entry.interactions.EntryDestructiveRemovalFeature
import mihon.entry.interactions.EntryDestructiveRemovalResult
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ProfileManager(
    private val application: Application,
    private val profileDatabase: ProfileDatabase = Injekt.get(),
    private val profileStore: ProfileStoreImpl = Injekt.get(),
    private val profilesPreferences: ProfilesPreferences = Injekt.get(),
    private val extensionManager: ExtensionManager = Injekt.get(),
    private val preferenceOwnership: ProfilePreferenceOwnership,
    private val entryRepository: EntryRepository = Injekt.get(),
    private val destructiveRemoval: EntryDestructiveRemovalFeature = Injekt.get(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val switchRequests = MutableStateFlow(profilesPreferences.activeProfileId.get())

    val activeProfileId: Long
        get() = profileStore.currentProfileId

    val profiles: StateFlow<List<Profile>> = profileDatabase.subscribeProfiles(includeArchived = true)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val visibleProfiles: StateFlow<List<Profile>> = profileDatabase.subscribeProfiles(includeArchived = false)
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val activeProfile: StateFlow<Profile?> = combine(profiles, switchRequests) { profiles, activeId ->
        profiles.firstOrNull { it.id == activeId }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val shouldShowPicker: Flow<Boolean> = profileDatabase.subscribeProfiles(includeArchived = false)
        .map { it.size > 1 }

    suspend fun shouldShowPickerOnLaunch(): Boolean {
        return profilesPreferences.pickerEnabled.get() && profileDatabase.getVisibleProfileCount() > 1
    }

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

    suspend fun loadInitialProfile(): Profile? {
        ensureDefaultProfile()
        val activeId = profilesPreferences.activeProfileId.get()
        val profile = profileDatabase.getProfileById(activeId)
            ?: visibleProfiles.value.firstOrNull()
            ?: profileDatabase.getProfileById(ProfileConstants.DEFAULT_PROFILE_ID)
        if (profile != null) {
            setActiveProfile(profile.id, rescheduleJobs = false)
        }
        return profile
    }

    suspend fun setActiveProfile(profileId: Long, rescheduleJobs: Boolean = true) {
        val profile = profileDatabase.getProfileById(profileId) ?: return
        profileStore.setCurrentProfileId(profile.id)
        switchRequests.value = profile.id
        if (rescheduleJobs) {
            LibraryUpdateJob.setupTask(application)
        }
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
        val hiddenSourceIds = extensionManager.installedExtensionsFlow.value
            .filterIsInstance<eu.kanade.tachiyomi.extension.model.Extension.Installed>()
            .flatMap { extension -> extension.sources.map { source -> source.id.toString() } }
            .toSet()
        profileStore.profileStore(id)
            .getStringSet(SourcePreferences.HIDDEN_SOURCES_KEY, hiddenSourceIds)
            .set(hiddenSourceIds)
        val customPreferences = CustomPreferences(profileStore.profileStore(id))
        customPreferences.homeScreenTabs.set(defaultHomeScreenTabs())
        customPreferences.homeScreenStartupTab.set(HomeScreenTabs.Library)
        seedDuplicateTitleExclusions(profileId = id)
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
    }

    suspend fun permanentlyDeleteProfile(profileId: Long) {
        if (profileId == ProfileConstants.DEFAULT_PROFILE_ID) return
        val profile = profileDatabase.getProfileById(profileId) ?: return
        if (!profile.isArchived) return
        val fallback = visibleProfiles.value.firstOrNull { it.id != profileId }
            ?: profileDatabase.getProfileById(ProfileConstants.DEFAULT_PROFILE_ID)
        if (activeProfileId == profileId && fallback != null) {
            setActiveProfile(fallback.id)
        }
        when (val result = destructiveRemoval.remove(entryRepository.getAllEntriesByProfile(profileId))) {
            is EntryDestructiveRemovalResult.Failed -> throw result.cause
            EntryDestructiveRemovalResult.NoChange -> Unit
            is EntryDestructiveRemovalResult.Removed -> {
                result.failures.forEach { failure ->
                    logcat(LogPriority.ERROR, failure.cause) {
                        "Profile deletion consequence ${failure.participantId} failed"
                    }
                }
            }
        }
        profileDatabase.deleteProfile(profileId)
        profileStore.deleteProfileState(profileId)
    }

    fun profileRequiresUnlock(profileId: Long): Boolean {
        return SecurityPreferences(profileStore.profileStore(profileId)).useAuthenticator.get()
    }

    private suspend fun migrateLegacyPreferencesIfNeeded() {
        val currentVersion = profilesPreferences.legacyPreferenceMigrationVersion.get()
        if (currentVersion >= LEGACY_PROFILE_MIGRATION_VERSION) return

        correctProfileOwnershipMismatches(currentVersion)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
        val ownership = preferenceOwnership.derive(sharedPreferences.all.keys)
        val migration = ProfilePreferenceMigration(sharedPreferences)
        migration.migrateLegacyPreferenceKeys(
            profileId = ProfileConstants.DEFAULT_PROFILE_ID,
            profileKeys = ownership.profile,
            appStateKeys = ownership.appState,
            privateKeys = ownership.private,
        )
        migrateLegacyProfileUnlockSettings()
        migrateLegacySourcePreferences()
        seedDuplicateTitleExclusionsForProfiles(
            profileDatabase.getProfiles(includeArchived = true)
                .map(Profile::id)
                .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) },
        )
        migration.cleanupLegacyPreferenceKeys(
            profileId = ProfileConstants.DEFAULT_PROFILE_ID,
            profileKeys = ownership.profile,
            appStateKeys = ownership.appState,
            privateKeys = ownership.private,
        )
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

    private suspend fun correctProfileOwnershipMismatches(currentVersion: Int) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
        if (currentVersion < 2) {
            migrateProfileOwnedKeyToNamespaced(
                sharedPreferences = sharedPreferences,
                baseKey = "show_nsfw_source",
            )
            migrateProfileOwnedKeyToNamespaced(
                sharedPreferences = sharedPreferences,
                baseKey = Preference.appStateKey("home_screen_startup_tab"),
            )
        }
        if (currentVersion < 7) {
            migrateProfileOwnedKeyToNamespaced(
                sharedPreferences = sharedPreferences,
                baseKey = Preference.appStateKey("home_screen_tab_order"),
            )
        }
        if (currentVersion < 5) {
            migrateProfileOwnedKeyToNamespaced(
                sharedPreferences = sharedPreferences,
                baseKey = Preference.appStateKey("pref_downloaded_only"),
            )
        }
        if (currentVersion < 6) {
            migrateLegacyProfileShortcutToHomeTabs(sharedPreferences)
        }
        if (currentVersion < 8) {
            migrateLegacyDuplicateDetectionPreferences(sharedPreferences)
        }
        if (currentVersion < 9) {
            migrateCustomPreferencesToProfileKeys(sharedPreferences)
        }
        if (currentVersion < 11) {
            migrateViewerSettingsToProfileKeys(sharedPreferences)
        }

        migrateKeyBackToGlobal(
            sharedPreferences = sharedPreferences,
            baseKey = "mark_duplicate_read_chapter_read",
        )
        migrateKeyBackToGlobal(
            sharedPreferences = sharedPreferences,
            baseKey = "anilist_score_type",
        )
        migrateKeyBackToGlobal(
            sharedPreferences = sharedPreferences,
            baseKey = "auto_clear_chapter_cache",
        )
        migrateKeyBackToGlobal(
            sharedPreferences = sharedPreferences,
            baseKey = "disallow_non_ascii_filenames",
        )
        sharedPreferences.edit(commit = true) {
            remove(Preference.appStateKey("profiles_switch_shortcut_enabled"))
        }
    }

    private suspend fun migrateLegacyDuplicateDetectionPreferences(
        sharedPreferences: android.content.SharedPreferences,
    ) {
        if (DuplicatePreferences.profileKeys.none(sharedPreferences::contains)) return

        val profileIds = profileDatabase.getProfiles(includeArchived = true)
            .map(Profile::id)
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        ProfilePreferenceMigration(sharedPreferences).copyLegacyPreferenceKeysToProfiles(
            profileIds = profileIds,
            profileKeys = DuplicatePreferences.profileKeys,
        )
    }

    private suspend fun migrateCustomPreferencesToProfileKeys(
        sharedPreferences: android.content.SharedPreferences,
    ) {
        val profileIds = profileDatabase.getProfiles(includeArchived = true)
            .map(Profile::id)
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            val customProfileKeys = CustomPreferences.profileKeys +
                EntryInteractionPreferences.profileKeys
            customProfileKeys.forEach { key ->
                migrateProfileAppStateKeyToProfileKey(sharedPreferences, profileId, key)
            }
        }
    }

    private suspend fun migrateViewerSettingsToProfileKeys(
        sharedPreferences: android.content.SharedPreferences,
    ) {
        val profileIds = profileDatabase.getProfiles(includeArchived = true)
            .map(Profile::id)
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }
        val profileKeys = preferenceOwnership.derive(
            existingKeys = sharedPreferences.all.keys,
            group = ProfilePreferenceOwnerGroupId(ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID),
        ).profile
        profileIds.forEach { profileId ->
            profileKeys.forEach { key -> migrateProfileAppStateKeyToProfileKey(sharedPreferences, profileId, key) }
        }
    }

    private fun migrateProfileAppStateKeyToProfileKey(
        sharedPreferences: android.content.SharedPreferences,
        profileId: Long,
        key: String,
    ) {
        val sourceKey = ProfileAwarePreferenceStore.Namespace.namespacedKey(
            Preference.appStateKey(key),
            profileId,
        )
        if (!sharedPreferences.contains(sourceKey)) return

        val targetKey = ProfileAwarePreferenceStore.Namespace.namespacedKey(key, profileId)
        sharedPreferences.edit(commit = true) {
            if (!sharedPreferences.contains(targetKey)) {
                when (val value = sharedPreferences.all[sourceKey]) {
                    is String -> putString(targetKey, value)
                    is Int -> putInt(targetKey, value)
                    is Long -> putLong(targetKey, value)
                    is Float -> putFloat(targetKey, value)
                    is Boolean -> putBoolean(targetKey, value)
                    is Set<*> -> putStringSet(targetKey, value.filterIsInstance<String>().toSet())
                }
            }
            remove(sourceKey)
        }
    }

    private fun seedDuplicateTitleExclusionsForProfiles(profileIds: Iterable<Long>) {
        profileIds.forEach(::seedDuplicateTitleExclusions)
    }

    private fun seedDuplicateTitleExclusions(profileId: Long) {
        val preference = DuplicatePreferences(profileStore.profileStore(profileId)).titleExclusionPatterns
        if (preference.isSet()) return
        preference.set(DuplicateTitleExclusions.defaultPatterns)
    }

    private fun migrateLegacyProfileShortcutToHomeTabs(
        sharedPreferences: android.content.SharedPreferences,
    ) {
        val legacyKey = Preference.appStateKey("profiles_switch_shortcut_enabled")
        if (!sharedPreferences.contains(legacyKey) || sharedPreferences.getBoolean(legacyKey, false).not()) return

        val profileIds = runCatching {
            kotlinx.coroutines.runBlocking { profileDatabase.getProfiles(includeArchived = true) }
        }
            .getOrDefault(emptyList())
            .map(Profile::id)
            .ifEmpty { listOf(ProfileConstants.DEFAULT_PROFILE_ID) }

        profileIds.forEach { profileId ->
            val customPreferences = CustomPreferences(profileStore.profileStore(profileId))
            val updatedTabs = (customPreferences.homeScreenTabs.get().ifEmpty { defaultHomeScreenTabs() })
                .toMutableSet()
                .apply { add(HomeScreenTabs.Profiles.name) }
                .mapNotNullTo(linkedSetOf()) { name ->
                    HomeScreenTabs.entries.find { it.name == name }
                }
            customPreferences.homeScreenTabs.set(updatedTabs.toHomeScreenTabPreferenceValue())
        }
    }

    private fun migrateProfileOwnedKeyToNamespaced(
        sharedPreferences: android.content.SharedPreferences,
        baseKey: String,
    ) {
        val namespacedKey = ProfileAwarePreferenceStore.Namespace.namespacedKey(
            baseKey,
            ProfileConstants.DEFAULT_PROFILE_ID,
        )
        if (!sharedPreferences.contains(baseKey) || sharedPreferences.contains(namespacedKey)) return

        sharedPreferences.edit(commit = true) {
            when (val value = sharedPreferences.all[baseKey]) {
                is String -> putString(namespacedKey, value)
                is Int -> putInt(namespacedKey, value)
                is Long -> putLong(namespacedKey, value)
                is Float -> putFloat(namespacedKey, value)
                is Boolean -> putBoolean(namespacedKey, value)
                is Set<*> -> putStringSet(namespacedKey, value.filterIsInstance<String>().toSet())
            }
            remove(baseKey)
        }
    }

    private fun migrateKeyBackToGlobal(
        sharedPreferences: android.content.SharedPreferences,
        baseKey: String,
    ) {
        val namespacedKey = ProfileAwarePreferenceStore.Namespace.namespacedKey(
            baseKey,
            ProfileConstants.DEFAULT_PROFILE_ID,
        )
        if (!sharedPreferences.contains(namespacedKey)) return

        sharedPreferences.edit(commit = true) {
            if (!sharedPreferences.contains(baseKey)) {
                when (val value = sharedPreferences.all[namespacedKey]) {
                    is String -> putString(baseKey, value)
                    is Int -> putInt(baseKey, value)
                    is Long -> putLong(baseKey, value)
                    is Float -> putFloat(baseKey, value)
                    is Boolean -> putBoolean(baseKey, value)
                    is Set<*> -> putStringSet(baseKey, value.filterIsInstance<String>().toSet())
                }
            }
            remove(namespacedKey)
        }
    }

    private fun migrateLegacySourcePreferences() {
        migrateLegacySourceListPreferences()

        val sharedPrefsDir = File(application.applicationInfo.dataDir, "shared_prefs")
        val sourceIds = sharedPrefsDir.listFiles()
            ?.asSequence()
            ?.map { it.name.removeSuffix(".xml") }
            ?.mapNotNull { name ->
                name.removePrefix("source_")
                    .takeIf { name.startsWith("source_") }
                    ?.toLongOrNull()
            }
            ?.toSet()
            .orEmpty()

        sourceIds.forEach { sourceId ->
            migrateLegacySourcePreferences(sourceId)
        }
    }

    private fun migrateLegacySourcePreferences(sourceId: Long) {
        val legacyName = "source_$sourceId"
        val legacyPrefs = application.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        if (legacyPrefs.all.isEmpty()) return

        val targetName = profileStore.sourcePreferenceKey(sourceId, ProfileConstants.DEFAULT_PROFILE_ID)
        val targetPrefs = application.getSharedPreferences(targetName, Context.MODE_PRIVATE)

        targetPrefs.edit(commit = true) {
            legacyPrefs.all.forEach { (key, value) ->
                if (targetPrefs.contains(key)) return@forEach

                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
        }

        if (targetPrefs.all.isNotEmpty()) {
            application.deleteSharedPreferences(legacyName)
        }
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

    fun storePendingIntent(intent: Intent?) {
        if (intent == null) {
            profilesPreferences.pendingIntentUri.delete()
            return
        }
        profilesPreferences.pendingIntentUri.set(intent.toUri(Intent.URI_INTENT_SCHEME))
    }

    fun consumePendingIntent(): Intent? {
        val intentUri = profilesPreferences.pendingIntentUri.get()
            .takeIf { it.isNotBlank() }
            ?: return null
        profilesPreferences.pendingIntentUri.delete()
        return runCatching {
            Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME)
        }.getOrNull()
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
        private const val LEGACY_PROFILE_MIGRATION_VERSION = 11
    }
}
