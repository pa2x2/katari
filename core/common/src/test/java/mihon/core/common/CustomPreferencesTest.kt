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
    fun `browse long press action defaults to library action`() {
        val preferences = CustomPreferences(TestPreferenceStore())

        preferences.browseLongPressAction.get() shouldBe CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION
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
