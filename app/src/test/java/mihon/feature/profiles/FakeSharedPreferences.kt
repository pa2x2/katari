package mihon.feature.profiles

import android.content.SharedPreferences

internal class FakeSharedPreferences : SharedPreferences {
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
