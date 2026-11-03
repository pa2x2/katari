package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.util.system.logcat

/**
 * A value of a [DesktopPreferenceStore], read and written with the semantics of [AndroidPreference]: a stored value of
 * another type than the one asked for is deleted, and one an object preference cannot decode reads as the default.
 */
internal sealed class DesktopPreference<T>(
    internal val store: DesktopPreferenceStore,
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {

    /** Decodes the value stored under this key, throwing [ClassCastException] when it has another type. */
    abstract fun decode(stored: Any): T

    abstract fun encode(value: T): Any

    override fun key(): String = key

    override fun get(): T {
        val stored = store.values.value[key] ?: return defaultValue
        return try {
            decode(stored)
        } catch (_: ClassCastException) {
            logcat { "Invalid value for $key; deleting" }
            delete()
            defaultValue
        }
    }

    override fun set(value: T) {
        store.put(key, encode(value))
    }

    override fun isSet(): Boolean = key in store.values.value

    override fun delete() {
        store.remove(key)
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> {
        return store.values
            .map { it[key] }
            .distinctUntilChanged()
            .map { get() }
            .conflate()
    }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> {
        return changes().stateIn(scope, SharingStarted.Eagerly, get())
    }

    class StringPrimitive(
        store: DesktopPreferenceStore,
        key: String,
        defaultValue: String,
    ) : DesktopPreference<String>(store, key, defaultValue) {
        override fun decode(stored: Any): String = stored as String

        override fun encode(value: String): Any = value
    }

    class LongPrimitive(
        store: DesktopPreferenceStore,
        key: String,
        defaultValue: Long,
    ) : DesktopPreference<Long>(store, key, defaultValue) {
        override fun decode(stored: Any): Long = stored as Long

        override fun encode(value: Long): Any = value
    }

    class IntPrimitive(
        store: DesktopPreferenceStore,
        key: String,
        defaultValue: Int,
    ) : DesktopPreference<Int>(store, key, defaultValue) {
        override fun decode(stored: Any): Int = stored as Int

        override fun encode(value: Int): Any = value
    }

    class FloatPrimitive(
        store: DesktopPreferenceStore,
        key: String,
        defaultValue: Float,
    ) : DesktopPreference<Float>(store, key, defaultValue) {
        override fun decode(stored: Any): Float = stored as Float

        override fun encode(value: Float): Any = value
    }

    class BooleanPrimitive(
        store: DesktopPreferenceStore,
        key: String,
        defaultValue: Boolean,
    ) : DesktopPreference<Boolean>(store, key, defaultValue) {
        override fun decode(stored: Any): Boolean = stored as Boolean

        override fun encode(value: Boolean): Any = value
    }

    class StringSetPrimitive(
        store: DesktopPreferenceStore,
        key: String,
        defaultValue: Set<String>,
    ) : DesktopPreference<Set<String>>(store, key, defaultValue) {
        override fun decode(stored: Any): Set<String> = stored.asStringSet()

        override fun encode(value: Set<String>): Any = value.toSet()
    }

    class ObjectAsString<T>(
        store: DesktopPreferenceStore,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> String,
        private val deserializer: (String) -> T,
    ) : DesktopPreference<T>(store, key, defaultValue) {
        override fun decode(stored: Any): T = try {
            deserializer(stored as String) ?: defaultValue()
        } catch (_: Exception) {
            defaultValue()
        }

        override fun encode(value: T): Any = serializer(value)
    }

    class ObjectAsInt<T>(
        store: DesktopPreferenceStore,
        key: String,
        defaultValue: T,
        private val serializer: (T) -> Int,
        private val deserializer: (Int) -> T,
    ) : DesktopPreference<T>(store, key, defaultValue) {
        override fun decode(stored: Any): T = try {
            deserializer(stored as Int)
        } catch (_: Exception) {
            defaultValue()
        }

        override fun encode(value: T): Any = serializer(value)
    }

    class ObjectSetAsStringSet<T>(
        store: DesktopPreferenceStore,
        key: String,
        defaultValue: Set<T>,
        private val serializer: (T) -> String,
        private val deserializer: (String) -> T?,
    ) : DesktopPreference<Set<T>>(store, key, defaultValue) {
        override fun decode(stored: Any): Set<T> = try {
            stored.asStringSet().mapNotNull(deserializer).toSet()
        } catch (_: Exception) {
            defaultValue()
        }

        override fun encode(value: Set<T>): Any = value.map(serializer).toSet()
    }
}

private fun Any.asStringSet(): Set<String> = (this as Set<*>).mapTo(LinkedHashSet()) { it as String }
