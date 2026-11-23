package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Reads [values] as a store holds them, by key, the way the platform stores read theirs: a missing or unreadable value
 * reads as the preference's default, and objects go through the preference's own deserializer. It reads a value that
 * isn't in a store, such as the one a setting had before a sync changed it, through the preference classes that own
 * its type; nothing can change it.
 */
class StoredValuesPreferenceStore(private val values: Map<String, Any?>) : PreferenceStore {

    override fun getString(key: String, defaultValue: String): Preference<String> = read(key, defaultValue) {
        it as? String
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> = read(key, defaultValue) { it as? Long }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> = read(key, defaultValue) { it as? Int }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> = read(key, defaultValue) {
        it as? Float
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        read(key, defaultValue) { it as? Boolean }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        read(key, defaultValue) { stored -> (stored as? Set<*>)?.filterIsInstance<String>()?.toSet() }

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = read(key, defaultValue) { stored -> (stored as? String)?.let(deserializer) }

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> = read(key, defaultValue) { stored ->
        (stored as? Set<*>)?.filterIsInstance<String>()?.mapNotNull(deserializer)?.toSet()
    }

    override fun getAll(): Map<String, *> = values

    private fun <T> read(key: String, defaultValue: T, decode: (Any) -> T?): Preference<T> {
        val value = values[key]?.let { stored -> runCatching { decode(stored) }.getOrNull() }
        return StoredValue(key, value, defaultValue)
    }

    private class StoredValue<T>(private val key: String, private val value: T?, private val default: T) :
        Preference<T> {
        override fun key(): String = key

        override fun get(): T = value ?: default

        override fun set(value: T) = throw UnsupportedOperationException("Stored values are read only")

        override fun isSet(): Boolean = value != null

        override fun delete() = throw UnsupportedOperationException("Stored values are read only")

        override fun defaultValue(): T = default

        override fun changes(): Flow<T> = flowOf(get())

        override fun stateIn(scope: CoroutineScope): StateFlow<T> = MutableStateFlow(get())
    }
}
