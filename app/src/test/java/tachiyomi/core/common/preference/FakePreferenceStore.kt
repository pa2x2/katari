package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Unlike [InMemoryPreferenceStore], every lookup of a key returns the same preference, so a value set through one
 * lookup is read back through the next, as with SharedPreferences.
 */
internal class FakePreferenceStore : PreferenceStore {
    private val values = mutableMapOf<String, FakePreference<*>>()

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
        return values.getOrPut(key) { FakePreference(key, defaultValue) } as Preference<T>
    }
}

private class FakePreference<T>(
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
