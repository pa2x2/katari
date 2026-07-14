package mihon.core.common

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class CustomPreferencesTest {

    @Test
    fun `browse long press action priority defaults to library then preview then immersive`() {
        val preferences = CustomPreferences(TestPreferenceStore())

        preferences.browseLongPressActionPriority.get() shouldBe listOf(
            CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
            CustomPreferences.BrowseLongPressAction.PREVIEW,
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
        )
    }

    @Test
    fun `legacy browse long press choice becomes first priority and missing actions are appended`() {
        "MANGA_PREVIEW".toBrowseLongPressActionPriority() shouldBe listOf(
            CustomPreferences.BrowseLongPressAction.PREVIEW,
            CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
        )
    }

    @Test
    fun `browse long press action priority removes duplicates and round trips`() {
        val priority = listOf(
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
            CustomPreferences.BrowseLongPressAction.PREVIEW,
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
        )

        priority.toBrowseLongPressActionPriorityPreferenceValue().toBrowseLongPressActionPriority() shouldBe listOf(
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
            CustomPreferences.BrowseLongPressAction.PREVIEW,
            CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
        )
    }

    @Test
    fun `browse long press source overrides round trip and ignore malformed values`() {
        val overrides = mapOf(
            20L to listOf(
                CustomPreferences.BrowseLongPressAction.IMMERSIVE,
                CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
            ),
            10L to listOf(
                CustomPreferences.BrowseLongPressAction.PREVIEW,
                CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
            ),
        )

        overrides.toBrowseLongPressActionOverridesPreferenceValue().toBrowseLongPressActionOverrides() shouldBe
            mapOf(
                10L to listOf(
                    CustomPreferences.BrowseLongPressAction.PREVIEW,
                    CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
                    CustomPreferences.BrowseLongPressAction.IMMERSIVE,
                ),
                20L to listOf(
                    CustomPreferences.BrowseLongPressAction.IMMERSIVE,
                    CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
                    CustomPreferences.BrowseLongPressAction.PREVIEW,
                ),
            )
        "invalid:IMMERSIVE;30:UNKNOWN;40:PREVIEW".toBrowseLongPressActionOverrides() shouldBe mapOf(
            40L to listOf(
                CustomPreferences.BrowseLongPressAction.PREVIEW,
                CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
                CustomPreferences.BrowseLongPressAction.IMMERSIVE,
            ),
        )
    }

    @Test
    fun `source priority uses override and falls back after override is cleared`() {
        val preferences = CustomPreferences(TestPreferenceStore())
        val sourceId = 42L
        val immersiveFirst = listOf(
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
            CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
            CustomPreferences.BrowseLongPressAction.PREVIEW,
        )

        preferences.browseLongPressActionPriority(sourceId) shouldBe defaultBrowseLongPressActionPriority()
        preferences.setBrowseLongPressActionOverride(sourceId, immersiveFirst)
        preferences.browseLongPressActionPriority(sourceId) shouldBe immersiveFirst
        preferences.clearBrowseLongPressActionOverride(sourceId)
        preferences.browseLongPressActionPriority(sourceId) shouldBe defaultBrowseLongPressActionPriority()
    }

    @Test
    fun `profile keys do not include anime player preferences`() {
        CustomPreferences.profileKeys.contains("enable_anime_picture_in_picture") shouldBe false
        CustomPreferences.profileKeys.contains("enable_anime_seek_preview") shouldBe false
    }

    private class TestPreferenceStore : PreferenceStore {
        private val values = mutableMapOf<String, TestPreference<*>>()

        override fun getString(key: String, defaultValue: String): Preference<String> = preference(key, defaultValue)

        override fun getLong(key: String, defaultValue: Long): Preference<Long> = preference(key, defaultValue)

        override fun getInt(key: String, defaultValue: Int): Preference<Int> = preference(key, defaultValue)

        override fun getFloat(key: String, defaultValue: Float): Preference<Float> = preference(key, defaultValue)

        override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> = preference(key, defaultValue)

        override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
            preference(key, defaultValue)

        override fun <T> getObjectFromString(
            key: String,
            defaultValue: T,
            serializer: (T) -> String,
            deserializer: (String) -> T,
        ): Preference<T> = preference(key, defaultValue)

        override fun <T> getObjectFromInt(
            key: String,
            defaultValue: T,
            serializer: (T) -> Int,
            deserializer: (Int) -> T,
        ): Preference<T> = preference(key, defaultValue)

        override fun <T> getObjectSetFromStringSet(
            key: String,
            defaultValue: Set<T>,
            serializer: (T) -> String,
            deserializer: (String) -> T?,
        ): Preference<Set<T>> = preference(key, defaultValue)

        override fun getAll(): Map<String, *> {
            return values.mapValues { it.value.get() }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> preference(key: String, defaultValue: T): TestPreference<T> {
            return values.getOrPut(key) { TestPreference(key, defaultValue) } as TestPreference<T>
        }
    }

    private class TestPreference<T>(
        private val preferenceKey: String,
        private val initialDefault: T,
    ) : Preference<T> {
        private val state = MutableStateFlow<T?>(null)

        override fun key(): String = preferenceKey

        override fun get(): T = state.value ?: initialDefault

        override fun set(value: T) {
            state.value = value
        }

        override fun isSet(): Boolean = state.value != null

        override fun delete() {
            state.value = null
        }

        override fun defaultValue(): T = initialDefault

        override fun changes(): Flow<T> {
            return state.asStateFlow().map { it ?: initialDefault }
        }

        override fun stateIn(scope: CoroutineScope): StateFlow<T> {
            return changes().stateIn(scope, SharingStarted.Eagerly, get())
        }
    }
}
