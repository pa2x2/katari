package mihon.core.migration.migrations

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.core.migration.MigrationContext
import mihon.entry.interactions.reader.settings.ReaderPreferences
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class VerticalNavigatorMigrationTest {

    @Test
    fun `migrates navigator preferences for every profile`() = runTest {
        val first = TestPreferenceStore()
        val second = TestPreferenceStore()
        first.getBoolean(OLD_VERTICAL_NAVIGATOR, true).set(false)
        first.getBoolean(OLD_VERTICAL_NAVIGATOR_ON_LEFT, false).set(true)
        second.getBoolean(OLD_VERTICAL_NAVIGATOR, true).set(true)
        val profileStore = TestProfileStore(mapOf(1L to first, 2L to second))
        val profileDatabase = mockk<ProfileDatabase>()
        coEvery { profileDatabase.getProfiles(includeArchived = true) } returns listOf(profile(1), profile(2))
        val context = MigrationContext(
            dryrun = false,
            previousVersion = 90,
            dependencies = mapOf(
                ProfileStore::class.java to profileStore,
                ProfileDatabase::class.java to profileDatabase,
            ),
        )

        assertTrue(VerticalNavigatorMigration().invoke(context))

        assertEquals(emptySet<ReadingMode>(), ReaderPreferences(first).verticalNavigator.get())
        assertTrue(ReaderPreferences(first).verticalNavigatorOnLeft.get())
        assertEquals(
            setOf(ReadingMode.WEBTOON, ReadingMode.CONTINUOUS_VERTICAL),
            ReaderPreferences(second).verticalNavigator.get(),
        )
        assertFalse(first.getBoolean(OLD_VERTICAL_NAVIGATOR, true).isSet())
        assertFalse(first.getBoolean(OLD_VERTICAL_NAVIGATOR_ON_LEFT, false).isSet())
        assertFalse(second.getBoolean(OLD_VERTICAL_NAVIGATOR, true).isSet())
    }

    @Test
    fun `migration is assigned to released fork upgrade version`() {
        assertEquals(91f, VerticalNavigatorMigration().version)
    }

    private fun profile(id: Long) = Profile(id, "uuid-$id", "Profile $id", 0, id, false, false)

    private class TestProfileStore(
        private val stores: Map<Long, PreferenceStore>,
    ) : ProfileStore {
        override val currentProfileId: Long = stores.keys.first()
        override val currentProfileIdFlow: Flow<Long> = flowOf(currentProfileId)
        override fun setCurrentProfileId(profileId: Long) = Unit
        override fun basePreferenceStore(): PreferenceStore = stores.getValue(currentProfileId)
        override fun appStateStore(): PreferenceStore = stores.getValue(currentProfileId)
        override fun privateStore(): PreferenceStore = stores.getValue(currentProfileId)
        override fun profileStore(): PreferenceStore = stores.getValue(currentProfileId)
        override fun profileStore(profileId: Long): PreferenceStore = stores.getValue(profileId)
        override fun appStateStore(profileId: Long): PreferenceStore = stores.getValue(profileId)
        override fun privateStore(profileId: Long): PreferenceStore = stores.getValue(profileId)
        override fun sourcePreferenceKey(sourceId: Long, profileId: Long): String = "source_${profileId}_$sourceId"
    }

    private class TestPreferenceStore : PreferenceStore {
        private val values = mutableMapOf<String, TestPreference<*>>()

        override fun getString(key: String, defaultValue: String) = preference(key, defaultValue)
        override fun getLong(key: String, defaultValue: Long) = preference(key, defaultValue)
        override fun getInt(key: String, defaultValue: Int) = preference(key, defaultValue)
        override fun getFloat(key: String, defaultValue: Float) = preference(key, defaultValue)
        override fun getBoolean(key: String, defaultValue: Boolean) = preference(key, defaultValue)
        override fun getStringSet(key: String, defaultValue: Set<String>) = preference(key, defaultValue)

        override fun <T> getObjectFromString(
            key: String,
            defaultValue: T,
            serializer: (T) -> String,
            deserializer: (String) -> T,
        ) = preference(key, defaultValue)

        override fun <T> getObjectFromInt(
            key: String,
            defaultValue: T,
            serializer: (T) -> Int,
            deserializer: (Int) -> T,
        ) = preference(key, defaultValue)

        override fun <T> getObjectSetFromStringSet(
            key: String,
            defaultValue: Set<T>,
            serializer: (T) -> String,
            deserializer: (String) -> T?,
        ) = preference(key, defaultValue)

        override fun getAll(): Map<String, *> = values.filterValues { it.isSet() }.mapValues { it.value.get() }

        @Suppress("UNCHECKED_CAST")
        private fun <T> preference(key: String, defaultValue: T): Preference<T> {
            return values.getOrPut(key) { TestPreference(key, defaultValue) } as Preference<T>
        }
    }

    private class TestPreference<T>(
        private val key: String,
        private val defaultValue: T,
    ) : Preference<T> {
        private var isSet = false
        private val state = MutableStateFlow(defaultValue)

        override fun key(): String = key
        override fun get(): T = state.value
        override fun set(value: T) {
            isSet = true
            state.value = value
        }
        override fun isSet(): Boolean = isSet
        override fun delete() {
            isSet = false
            state.value = defaultValue
        }
        override fun defaultValue(): T = defaultValue
        override fun changes(): Flow<T> = state
        override fun stateIn(scope: CoroutineScope): StateFlow<T> = state
    }

    private companion object {
        const val OLD_VERTICAL_NAVIGATOR = "pref_webtoon_vertical_navigator"
        const val OLD_VERTICAL_NAVIGATOR_ON_LEFT = "pref_webtoon_vertical_navigator_on_left"
    }
}
