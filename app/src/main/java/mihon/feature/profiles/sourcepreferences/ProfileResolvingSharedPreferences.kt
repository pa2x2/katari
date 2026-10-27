package mihon.feature.profiles.sourcepreferences

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import tachiyomi.core.common.preference.SourcePreferenceProfile
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * A source's settings as extensions hold them: for the source's whole lifetime, across profile switches. So every call
 * picks the file of the profile it works for, [SourcePreferenceProfile.current] or else the active profile, and an
 * editor stays bound to the file it was taken from.
 *
 * Listeners follow the active profile's file. Android calls them on the main thread, outside any profile's work, where
 * a read resolves to the active profile as well. When the active profile changes they hear each key whose value differs
 * between the two files, never the `null` "cleared" key: extensions that declare the key non-null crash on it.
 */
internal class ProfileResolvingSharedPreferences(
    private val open: (profileId: Long) -> SharedPreferences,
    private val activeProfileId: () -> Long,
) : SharedPreferences {

    private val files = ConcurrentHashMap<Long, SharedPreferences>()

    // Weak, as in Android's own registry, so a listener lives exactly as long as it did on the plain file.
    private val listeners = WeakHashMap<OnSharedPreferenceChangeListener, Unit>()
    private var listenedProfileId: Long? = null
    private val forwarder = OnSharedPreferenceChangeListener { _, key -> notifyListeners(listOf(key)) }

    override fun getAll(): MutableMap<String, *> = file().all

    override fun getString(key: String?, defValue: String?): String? = file().getString(key, defValue)

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        file().getStringSet(key, defValues)

    override fun getInt(key: String?, defValue: Int): Int = file().getInt(key, defValue)

    override fun getLong(key: String?, defValue: Long): Long = file().getLong(key, defValue)

    override fun getFloat(key: String?, defValue: Float): Float = file().getFloat(key, defValue)

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = file().getBoolean(key, defValue)

    override fun contains(key: String?): Boolean = file().contains(key)

    override fun edit(): SharedPreferences.Editor = file().edit()

    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {
        if (listener == null) return
        synchronized(listeners) {
            listeners[listener] = Unit
            if (listenedProfileId == null) {
                val profileId = activeProfileId()
                file(profileId).registerOnSharedPreferenceChangeListener(forwarder)
                listenedProfileId = profileId
            }
        }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {
        synchronized(listeners) {
            listeners.remove(listener)
            val profileId = listenedProfileId
            if (listeners.isEmpty() && profileId != null) {
                file(profileId).unregisterOnSharedPreferenceChangeListener(forwarder)
                listenedProfileId = null
            }
        }
    }

    /** Moves the listeners onto [profileId]'s file and tells them which keys the switch changed. */
    fun onActiveProfileChanged(profileId: Long) {
        val changedKeys = synchronized(listeners) {
            val previousId = listenedProfileId
            if (previousId == null || previousId == profileId) return
            val previous = file(previousId)
            val current = file(profileId)
            previous.unregisterOnSharedPreferenceChangeListener(forwarder)
            current.registerOnSharedPreferenceChangeListener(forwarder)
            listenedProfileId = profileId
            val before = previous.all
            val after = current.all
            (before.keys + after.keys).filter { before[it] != after[it] }
        }
        notifyListeners(changedKeys)
    }

    /** Drops [profileId]'s file after it was deleted, so a profile that later reuses the ID opens a fresh one. */
    fun forget(profileId: Long) {
        files.remove(profileId)
    }

    private fun file(): SharedPreferences = file(SourcePreferenceProfile.current ?: activeProfileId())

    private fun file(profileId: Long): SharedPreferences = files.getOrPut(profileId) { open(profileId) }

    private fun notifyListeners(keys: List<String?>) {
        if (keys.isEmpty()) return
        val current = synchronized(listeners) { listeners.keys.toList() }
        keys.forEach { key -> current.forEach { it.onSharedPreferenceChanged(this, key) } }
    }
}
