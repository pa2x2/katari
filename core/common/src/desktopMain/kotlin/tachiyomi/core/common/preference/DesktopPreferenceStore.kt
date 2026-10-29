package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.DesktopPreference.BooleanPrimitive
import tachiyomi.core.common.preference.DesktopPreference.FloatPrimitive
import tachiyomi.core.common.preference.DesktopPreference.IntPrimitive
import tachiyomi.core.common.preference.DesktopPreference.LongPrimitive
import tachiyomi.core.common.preference.DesktopPreference.ObjectAsInt
import tachiyomi.core.common.preference.DesktopPreference.ObjectAsString
import tachiyomi.core.common.preference.DesktopPreference.ObjectSetAsStringSet
import tachiyomi.core.common.preference.DesktopPreference.StringPrimitive
import tachiyomi.core.common.preference.DesktopPreference.StringSetPrimitive
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Preferences kept in one file, as [AndroidPreferenceStore] keeps them in shared preferences: a change is visible at
 * once and written to disk in the background. Writes that are still pending when the JVM exits are written by a
 * shutdown hook, since the background writer runs on daemon threads that do not hold the exit back.
 */
class DesktopPreferenceStore(path: Path) : PreferenceStore {

    private val file = DesktopPreferenceFile(path)
    private val state = MutableStateFlow(file.read())
    private val unwritten = AtomicBoolean(false)
    private val writeRequests = Channel<Unit>(Channel.CONFLATED)
    private val writeLock = Any()

    internal val values: StateFlow<Map<String, Any>> = state.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            writeRequests.consumeEach { flush() }
        }
        Runtime.getRuntime().addShutdownHook(Thread({ flush() }, "Preference writer"))
    }

    internal fun put(key: String, value: Any) {
        val previous = state.getAndUpdate { it + (key to value) }
        if (previous[key] != value) requestWrite()
    }

    internal fun remove(key: String) {
        val previous = state.getAndUpdate { it - key }
        if (key in previous) requestWrite()
    }

    /**
     * Writes the current values if a change has not reached the file yet, blocking until it has. Returns whether every
     * change is in the file.
     */
    fun flush(): Boolean {
        synchronized(writeLock) {
            if (unwritten.getAndSet(false) && !file.write(state.value)) {
                unwritten.set(true)
                return false
            }
            return true
        }
    }

    /**
     * Removes every value and the file, leaving the store as one opened on a missing file. A write that is under way
     * finishes first, so it cannot bring the file back afterwards.
     */
    fun clear() {
        synchronized(writeLock) {
            state.value = emptyMap()
            unwritten.set(!file.delete())
        }
    }

    private fun requestWrite() {
        unwritten.set(true)
        writeRequests.trySend(Unit)
    }

    override fun getString(key: String, defaultValue: String): Preference<String> {
        return StringPrimitive(this, key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> {
        return LongPrimitive(this, key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> {
        return IntPrimitive(this, key, defaultValue)
    }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
        return FloatPrimitive(this, key, defaultValue)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        return BooleanPrimitive(this, key, defaultValue)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        return StringSetPrimitive(this, key, defaultValue)
    }

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> {
        return ObjectAsString(this, key, defaultValue, serializer, deserializer)
    }

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> {
        return ObjectAsInt(this, key, defaultValue, serializer, deserializer)
    }

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> {
        return ObjectSetAsStringSet(this, key, defaultValue, serializer, deserializer)
    }

    override fun getAll(): Map<String, *> = state.value

    /** Whether [preference] reads and writes this store. */
    fun owns(preference: Preference<*>): Boolean = preference is DesktopPreference<*> && preference.store === this
}
