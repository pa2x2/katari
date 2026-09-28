package mihon.feature.profiles.core

import android.content.Context
import android.content.SharedPreferences
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mihon.core.common.CustomPreferences
import mihon.core.common.HomeScreenTabs
import mihon.core.common.defaultHomeScreenTabOrder
import mihon.core.common.defaultHomeScreenTabs
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroupType
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions
import tachiyomi.domain.library.service.LibraryPreferences

class ProfileAwareLibraryPreferencesTest {

    @Test
    fun `settings of every storage kind stay isolated per profile`() {
        val fixture = createFixture()
        val sort = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Descending)
        val tabs = setOf(HomeScreenTabs.Library.name, HomeScreenTabs.More.name)

        fixture.libraryPreferences.defaultCategory.set(3)
        fixture.libraryPreferences.downloadedOnly.set(true)
        fixture.libraryPreferences.sortingMode.set(sort)
        fixture.customPreferences.homeScreenTabs.set(tabs)
        fixture.duplicatePreferences.titleExclusionPatterns.set(listOf("[*]"))

        fixture.activeProfileId.value = 2L

        fixture.libraryPreferences.defaultCategory.get() shouldBe -1
        fixture.libraryPreferences.downloadedOnly.get() shouldBe false
        fixture.libraryPreferences.sortingMode.get() shouldBe LibrarySort.default
        fixture.customPreferences.homeScreenTabs.get() shouldBe defaultHomeScreenTabs()
        fixture.duplicatePreferences.titleExclusionPatterns.get() shouldBe DuplicateTitleExclusions.defaultPatterns
        fixture.libraryPreferences.defaultCategory.set(7)

        fixture.activeProfileId.value = 1L

        fixture.libraryPreferences.defaultCategory.get() shouldBe 3
        fixture.libraryPreferences.downloadedOnly.get() shouldBe true
        fixture.libraryPreferences.sortingMode.get() shouldBe sort
        fixture.customPreferences.homeScreenTabs.get() shouldBe tabs
        fixture.duplicatePreferences.titleExclusionPatterns.get() shouldBe listOf("[*]")
    }

    @Test
    fun `library preference flow follows active profile`() = runTest {
        val fixture = createFixture()
        fixture.libraryPreferences.downloadedOnly.set(true)

        val values = mutableListOf<Boolean>()
        val job = launch {
            fixture.libraryPreferences.downloadedOnly.changes().take(4).toList(values)
        }

        advanceUntilIdle()
        values.last() shouldBe true

        fixture.activeProfileId.value = 2L
        advanceUntilIdle()
        values.last() shouldBe false

        fixture.libraryPreferences.downloadedOnly.set(true)
        advanceUntilIdle()
        values.last() shouldBe true

        job.cancel()
    }

    @Test
    fun `legacy duplicate detection settings copy to every profile before cleanup`() {
        val sharedPreferences = FakeSharedPreferences().apply {
            edit()
                .putBoolean("extended_duplicate_detection_enabled", true)
                .putInt("extended_duplicate_detection_minimum_match_score", 33)
                .putInt("extended_duplicate_detection_cover_weight", 12)
                .putString(
                    "extended_duplicate_detection_title_exclusion_patterns",
                    "[\"[*]\",\"(*)\"]",
                )
                .commit()
        }

        val migration = ProfilePreferenceMigration(sharedPreferences)
        val profileIds = listOf(1L, 2L, 5L)

        migration.copyLegacyPreferenceKeysToProfiles(
            profileIds = profileIds,
            profileKeys = DuplicatePreferences.profileKeys,
        )

        profileIds.forEach { profileId ->
            sharedPreferences.getBoolean(
                ProfileAwarePreferenceStore.Namespace.namespacedKey(
                    "extended_duplicate_detection_enabled",
                    profileId,
                ),
                false,
            ) shouldBe true
            sharedPreferences.getInt(
                ProfileAwarePreferenceStore.Namespace.namespacedKey(
                    "extended_duplicate_detection_minimum_match_score",
                    profileId,
                ),
                0,
            ) shouldBe 33
            sharedPreferences.getInt(
                ProfileAwarePreferenceStore.Namespace.namespacedKey(
                    "extended_duplicate_detection_cover_weight",
                    profileId,
                ),
                0,
            ) shouldBe 12
            sharedPreferences.getString(
                ProfileAwarePreferenceStore.Namespace.namespacedKey(
                    "extended_duplicate_detection_title_exclusion_patterns",
                    profileId,
                ),
                null,
            ) shouldBe "[\"[*]\",\"(*)\"]"
        }

        migration.cleanupLegacyPreferenceKeys(
            profileId = 1L,
            profileKeys = DuplicatePreferences.profileKeys,
        )

        sharedPreferences.contains("extended_duplicate_detection_enabled") shouldBe false
        sharedPreferences.contains("extended_duplicate_detection_minimum_match_score") shouldBe false
        sharedPreferences.contains("extended_duplicate_detection_cover_weight") shouldBe false
        sharedPreferences.contains("extended_duplicate_detection_title_exclusion_patterns") shouldBe false
    }

    @Test
    fun `installed static and dynamic owners drive migration and cleanup without a recorder list`() {
        val sharedPreferences = FakeSharedPreferences().apply {
            edit()
                .putBoolean("future_feature_enabled", true)
                .putString("future_feature_item_42", "remembered")
                .putInt(Preference.appStateKey("future_feature_cursor"), 7)
                .putString(Preference.privateKey("future_feature_token_9"), "secret")
                .commit()
        }
        val registry = ProfilePreferenceOwnerRegistry()
        val installer = ProfilePreferenceOwnerInstaller(registry) { InMemoryPreferenceStore() }
        installer.register(
            ProfilePreferenceOwnerId("test.future-feature"),
            factory = ::FutureFeaturePreferences,
        )
        installer.register(
            id = ProfilePreferenceOwnerId("test.future-feature-items"),
            keyPatterns = setOf(
                ProfilePreferenceKeyPattern.Prefix("future_feature_item_"),
                ProfilePreferenceKeyPattern.Prefix(Preference.privateKey("future_feature_token_")),
            ),
            factory = ::FutureFeatureItemPreferences,
        )

        val ownership = ProfilePreferenceOwnership(registry).derive(sharedPreferences.all.keys)
        val migration = ProfilePreferenceMigration(sharedPreferences)
        migration.migrateLegacyPreferenceKeys(
            profileId = 3L,
            profileKeys = ownership.profile,
            appStateKeys = ownership.appState,
            privateKeys = ownership.private,
        )
        migration.cleanupLegacyPreferenceKeys(
            profileId = 3L,
            profileKeys = ownership.profile,
            appStateKeys = ownership.appState,
            privateKeys = ownership.private,
        )

        sharedPreferences.getBoolean(namespaced("future_feature_enabled", 3L), false) shouldBe true
        sharedPreferences.getString(namespaced("future_feature_item_42", 3L), null) shouldBe "remembered"
        sharedPreferences.getInt(namespaced(Preference.appStateKey("future_feature_cursor"), 3L), 0) shouldBe 7
        sharedPreferences.getString(
            namespaced(Preference.privateKey("future_feature_token_9"), 3L),
            null,
        ) shouldBe "secret"
        sharedPreferences.contains("future_feature_enabled") shouldBe false
        sharedPreferences.contains("future_feature_item_42") shouldBe false
        sharedPreferences.contains(Preference.appStateKey("future_feature_cursor")) shouldBe false
        sharedPreferences.contains(Preference.privateKey("future_feature_token_9")) shouldBe false
    }

    private fun namespaced(key: String, profileId: Long): String {
        return ProfileAwarePreferenceStore.Namespace.namespacedKey(key, profileId)
    }

    private fun createFixture(): Fixture {
        val activeProfileId = MutableStateFlow(1L)
        val backing = AndroidPreferenceStore(
            context = mockk<Context>(relaxed = true),
            sharedPreferences = FakeSharedPreferences(),
        )
        val preferenceStore = ProfileAwarePreferenceStore(
            backing = backing,
            profileProvider = { activeProfileId.value },
            profileFlow = activeProfileId,
            namespace = ProfileAwarePreferenceStore.Namespace.PROFILE,
        )
        return Fixture(
            activeProfileId = activeProfileId,
            libraryPreferences = LibraryPreferences(preferenceStore),
            duplicatePreferences = DuplicatePreferences(preferenceStore),
            customPreferences = CustomPreferences(preferenceStore),
        )
    }

    private data class Fixture(
        val activeProfileId: MutableStateFlow<Long>,
        val libraryPreferences: LibraryPreferences,
        val duplicatePreferences: DuplicatePreferences,
        val customPreferences: CustomPreferences,
    )

    private class FakeSharedPreferences : SharedPreferences {
        private val data = linkedMapOf<String, Any?>()
        private val listeners = linkedSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): MutableMap<String, *> = LinkedHashMap(data)

        override fun getString(key: String?, defValue: String?): String? {
            return data[key] as? String ?: defValue
        }

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            val value = data[key] as? Set<String>
            return value?.toMutableSet() ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int {
            return data[key] as? Int ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            return data[key] as? Long ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            return data[key] as? Float ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return data[key] as? Boolean ?: defValue
        }

        override fun contains(key: String?): Boolean {
            return data.containsKey(key)
        }

        override fun edit(): SharedPreferences.Editor = Editor()

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            if (listener != null) listeners += listener
        }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            if (listener != null) listeners -= listener
        }

        private inner class Editor : SharedPreferences.Editor {
            private var clearRequested = false
            private val removals = linkedSetOf<String>()
            private val updates = linkedMapOf<String, Any?>()

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = values?.toSet()
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                if (key != null) updates[key] = value
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                if (key != null) removals += key
            }

            override fun clear(): SharedPreferences.Editor = apply {
                clearRequested = true
            }

            override fun commit(): Boolean {
                applyChanges()
                return true
            }

            override fun apply() {
                applyChanges()
            }

            private fun applyChanges() {
                val changedKeys = linkedSetOf<String>()
                if (clearRequested) {
                    changedKeys += data.keys
                    data.clear()
                }
                removals.forEach { key ->
                    if (data.remove(key) != null) {
                        changedKeys += key
                    }
                }
                updates.forEach { (key, value) ->
                    if (value == null) {
                        if (data.remove(key) != null) {
                            changedKeys += key
                        }
                    } else {
                        data[key] = value
                        changedKeys += key
                    }
                }
                changedKeys.forEach { key ->
                    listeners.forEach { listener ->
                        listener.onSharedPreferenceChanged(this@FakeSharedPreferences, key)
                    }
                }
            }
        }
    }
}

private class FutureFeaturePreferences(preferenceStore: PreferenceStore) {
    val enabled = preferenceStore.getBoolean("future_feature_enabled")
    val cursor = preferenceStore.getInt(Preference.appStateKey("future_feature_cursor"))
}

private class FutureFeatureItemPreferences(private val preferenceStore: PreferenceStore) {
    fun item(id: Long) = preferenceStore.getString("future_feature_item_$id")

    fun token(id: Long) = preferenceStore.getString(Preference.privateKey("future_feature_token_$id"))
}
