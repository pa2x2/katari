package app.katari.profiles.core

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import app.katari.core.common.CustomPreferences
import app.katari.core.common.HomeScreenTabs
import app.katari.core.common.defaultHomeScreenTabs
import app.katari.core.common.toHomeScreenTabPreferenceValue
import app.katari.entry.interactions.media.ENTRY_VIEWER_SETTINGS_LEGACY_PREFERENCE_OWNER_GROUP_ID
import app.katari.entry.interactions.settings.EntryInteractionPreferences
import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.ProfilePreferenceOwnerGroupId
import tachiyomi.domain.library.service.DuplicatePreferences

/**
 * Moves keys within Android's one shared-preferences file: first the corrections of the migration versions the app
 * has not run yet, then the keys from before profiles into the default profile's namespace, removing those old keys
 * only after the profile stores have read them.
 */
@Inject
class AndroidLegacyProfilePreferenceKeys(
    private val application: Application,
    private val profileDatabase: ProfileDatabase,
    private val profileStore: ProfileStore,
    private val preferenceOwnership: ProfilePreferenceOwnership,
) : LegacyProfilePreferenceKeys {

    override suspend fun migrate(fromVersion: Int, migrateProfileStores: suspend () -> Unit) {
        correctProfileOwnershipMismatches(fromVersion)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
        val ownership = preferenceOwnership.derive(sharedPreferences.all.keys)
        val migration = ProfilePreferenceMigration(sharedPreferences)
        migration.migrateLegacyPreferenceKeys(
            profileId = ProfileConstants.DEFAULT_PROFILE_ID,
            profileKeys = ownership.profile,
            appStateKeys = ownership.appState,
            privateKeys = ownership.private,
        )
        migrateProfileStores()
        migration.cleanupLegacyPreferenceKeys(
            profileId = ProfileConstants.DEFAULT_PROFILE_ID,
            profileKeys = ownership.profile,
            appStateKeys = ownership.appState,
            privateKeys = ownership.private,
        )
    }

    private suspend fun correctProfileOwnershipMismatches(currentVersion: Int) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
        if (currentVersion < 2) {
            migrateProfileOwnedKeyToNamespaced(
                sharedPreferences = sharedPreferences,
                baseKey = SourcePreferences.LEGACY_SHOW_NSFW_SOURCE_KEY,
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
        sharedPreferences: SharedPreferences,
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
        sharedPreferences: SharedPreferences,
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
        sharedPreferences: SharedPreferences,
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
        sharedPreferences: SharedPreferences,
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

    private fun migrateLegacyProfileShortcutToHomeTabs(
        sharedPreferences: SharedPreferences,
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
        sharedPreferences: SharedPreferences,
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
        sharedPreferences: SharedPreferences,
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
}
