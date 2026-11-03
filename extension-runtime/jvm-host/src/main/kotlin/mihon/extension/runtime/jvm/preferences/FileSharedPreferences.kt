package mihon.extension.runtime.jvm.preferences

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import tachiyomi.core.common.preference.DesktopPreferenceStore
import java.util.WeakHashMap
import java.util.concurrent.Executor

/**
 * Android's shared preferences over [store], the JSON file the desktop app keeps its own settings in, behaving as
 * Android's do: an editor's changes are visible once it commits or applies, all at once to other editors, `apply`
 * leaves writing the file to the background and `commit` waits for it. Listeners are held weakly and hear, on
 * [listenerThread], `null` when an editor cleared the file and then each key it changed, the last change first.
 *
 * [store] must have no other writer, so one instance serves each file.
 */
class FileSharedPreferences(
    val store: DesktopPreferenceStore,
    private val listenerThread: Executor,
) : SharedPreferences {

    // Editors commit one at a time, so one's clear can't land between another's changes
    private val commitLock = Any()
    private val listeners = WeakHashMap<OnSharedPreferenceChangeListener, Unit>()

    override fun getAll(): MutableMap<String, *> = HashMap(store.getAll())

    override fun getString(key: String?, defValue: String?): String? = store.getAll()[key] as String? ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val stored = store.getAll()[key] as Set<*>? ?: return defValues
        return stored.mapTo(LinkedHashSet()) { it as String }
    }

    override fun getInt(key: String?, defValue: Int): Int = store.getAll()[key] as Int? ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = store.getAll()[key] as Long? ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = store.getAll()[key] as Float? ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = store.getAll()[key] as Boolean? ?: defValue

    override fun contains(key: String?): Boolean = key in store.getAll()

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {
        if (listener == null) return
        synchronized(listeners) { listeners[listener] = Unit }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {
        synchronized(listeners) { listeners.remove(listener) }
    }

    /** Applies [changes], `null` removing a key, after removing every key when [clear]; returns the keys it changed. */
    private fun write(clear: Boolean, changes: Map<String, Any?>): Change {
        synchronized(commitLock) {
            val cleared = clear && store.getAll().isNotEmpty()
            if (cleared) store.clear()
            val current = store.getAll()
            val changed = changes.filter { (key, value) -> current[key] != value }.keys.toList()
            changed.forEach { key -> store.put(key, changes.getValue(key)) }
            return Change(cleared, changed)
        }
    }

    private fun DesktopPreferenceStore.put(key: String, value: Any?) {
        when (value) {
            null -> getString(key).delete()
            is String -> getString(key).set(value)
            is Set<*> -> getStringSet(key).set(value.mapTo(LinkedHashSet()) { it as String })
            is Int -> getInt(key).set(value)
            is Long -> getLong(key).set(value)
            is Float -> getFloat(key).set(value)
            is Boolean -> getBoolean(key).set(value)
            else -> error("Shared preferences can't hold a ${value::class.simpleName}")
        }
    }

    private fun notifyListeners(change: Change) {
        if (!change.cleared && change.keys.isEmpty()) return
        val current = synchronized(listeners) { listeners.keys.toList() }
        if (current.isEmpty()) return
        listenerThread.execute {
            if (change.cleared) current.forEach { it.onSharedPreferenceChanged(this, null) }
            change.keys.asReversed().forEach { key -> current.forEach { it.onSharedPreferenceChanged(this, key) } }
        }
    }

    private class Change(val cleared: Boolean, val keys: List<String>)

    private inner class Editor : SharedPreferences.Editor {
        private val changes = LinkedHashMap<String, Any?>()
        private var clear = false

        override fun putString(key: String, value: String?) = change(key, value)

        override fun putStringSet(key: String, values: MutableSet<String>?) = change(key, values?.toSet())

        override fun putInt(key: String, value: Int) = change(key, value)

        override fun putLong(key: String, value: Long) = change(key, value)

        override fun putFloat(key: String, value: Float) = change(key, value)

        override fun putBoolean(key: String, value: Boolean) = change(key, value)

        override fun remove(key: String) = change(key, null)

        override fun clear(): SharedPreferences.Editor {
            synchronized(this) { clear = true }
            return this
        }

        override fun commit(): Boolean {
            notifyListeners(take())
            return store.flush()
        }

        override fun apply() {
            notifyListeners(take())
        }

        private fun change(key: String, value: Any?): SharedPreferences.Editor {
            synchronized(this) { changes[key] = value }
            return this
        }

        private fun take(): Change {
            val (clear, changes) = synchronized(this) {
                (clear to LinkedHashMap(changes)).also {
                    this.clear = false
                    this.changes.clear()
                }
            }
            return write(clear, changes)
        }
    }
}
