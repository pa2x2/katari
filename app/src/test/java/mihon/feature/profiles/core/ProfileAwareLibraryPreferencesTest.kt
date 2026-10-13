package mihon.feature.profiles.core

import android.content.Context
import android.content.SharedPreferences
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.core.common.CustomPreferences
import mihon.core.common.HomeScreenTabs
import mihon.core.common.defaultHomeScreenTabs
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.AndroidPreferenceStore
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
