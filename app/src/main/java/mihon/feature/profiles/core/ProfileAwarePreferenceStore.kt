package mihon.feature.profiles.core

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ProfileAwarePreferenceStore(
    private val backing: AndroidPreferenceStore,
    private val profileProvider: () -> Long,
    private val profileFlow: Flow<Long>,
    private val namespace: Namespace,
) : PreferenceStore {

    override fun getString(key: String, defaultValue: String): Preference<String> {
        return DynamicPreference(
            preferences = backing.sharedPreferences,
            keyFlow = backing.keyFlow,
            profileFlow = profileFlow,
            defaultValue = defaultValue,
            keyProvider = { key(key) },
            read = { prefs, prefKey, default -> prefs.getString(prefKey, default) ?: default },
            write = { editor, prefKey, value -> editor.putString(prefKey, value) },
        )
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> {
        return DynamicPreference(
            preferences = backing.sharedPreferences,
            keyFlow = backing.keyFlow,
            profileFlow = profileFlow,
            defaultValue = defaultValue,
            keyProvider = { key(key) },
            read = { prefs, prefKey, default -> prefs.getLong(prefKey, default) },
            write = { editor, prefKey, value -> editor.putLong(prefKey, value) },
        )
    }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> {
        return DynamicPreference(
            preferences = backing.sharedPreferences,
            keyFlow = backing.keyFlow,
            profileFlow = profileFlow,
            defaultValue = defaultValue,
            keyProvider = { key(key) },
            read = { prefs, prefKey, default -> prefs.getInt(prefKey, default) },
            write = { editor, prefKey, value -> editor.putInt(prefKey, value) },
        )
    }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
        return DynamicPreference(
            preferences = backing.sharedPreferences,
            keyFlow = backing.keyFlow,
            profileFlow = profileFlow,
            defaultValue = defaultValue,
            keyProvider = { key(key) },
            read = { prefs, prefKey, default -> prefs.getFloat(prefKey, default) },
            write = { editor, prefKey, value -> editor.putFloat(prefKey, value) },
        )
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        return DynamicPreference(
            preferences = backing.sharedPreferences,
            keyFlow = backing.keyFlow,
            profileFlow = profileFlow,
            defaultValue = defaultValue,
            keyProvider = { key(key) },
            read = { prefs, prefKey, default -> prefs.getBoolean(prefKey, default) },
            write = { editor, prefKey, value -> editor.putBoolean(prefKey, value) },
        )
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        return DynamicPreference(
            preferences = backing.sharedPreferences,
            keyFlow = backing.keyFlow,
            profileFlow = profileFlow,
            defaultValue = defaultValue,
            keyProvider = { key(key) },
            read = { prefs, prefKey, default -> prefs.getStringSet(prefKey, default) ?: default },
            write = { editor, prefKey, value -> editor.putStringSet(prefKey, value) },
        )
    }

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> {
        return DynamicPreference(
            preferences = backing.sharedPreferences,
            keyFlow = backing.keyFlow,
            profileFlow = profileFlow,
            defaultValue = defaultValue,
            keyProvider = { key(key) },
            read = { prefs, prefKey, default ->
                try {
                    prefs.getString(prefKey, null)?.let(deserializer) ?: default
                } catch (_: Exception) {
                    default
                }
            },
            write = { editor, prefKey, value -> editor.putString(prefKey, serializer(value)) },
        )
    }

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> {
        return DynamicPreference(
            preferences = backing.sharedPreferences,
            keyFlow = backing.keyFlow,
            profileFlow = profileFlow,
            defaultValue = defaultValue,
            keyProvider = { key(key) },
            read = { prefs, prefKey, default ->
                try {
                    if (prefs.contains(prefKey)) {
                        serializer(default).let {
                            prefs.getInt(prefKey, it)
                        }.let(deserializer)
                    } else {
                        default
                    }
                } catch (_: Exception) {
                    default
                }
            },
            write = { editor, prefKey, value -> editor.putInt(prefKey, serializer(value)) },
        )
    }

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> {
        return DynamicPreference(
            preferences = backing.sharedPreferences,
            keyFlow = backing.keyFlow,
            profileFlow = profileFlow,
            defaultValue = defaultValue,
            keyProvider = { key(key) },
            read = { prefs, prefKey, default ->
                try {
                    prefs.getStringSet(prefKey, null)?.mapNotNull(deserializer)?.toSet() ?: default
                } catch (_: Exception) {
                    default
                }
            },
            write = { editor, prefKey, value -> editor.putStringSet(prefKey, value.map(serializer).toSet()) },
        )
    }

    override fun getAll(): Map<String, *> {
        val profileId = profileProvider()
        return backing.getAll()
            .filterKeys { namespace.matches(it, profileId) }
            .mapKeys { (key, _) -> namespace.stripPrefix(key, profileId) }
    }

    fun keys(profileId: Long = profileProvider()): Set<String> {
        return backing.getAll().keys.filterTo(mutableSetOf()) { key ->
            namespace.matches(key, profileId) && namespace.stripPrefix(key, profileId) != key
        }
    }

    private fun key(base: String): String {
        return namespace.key(base, profileProvider())
    }

    enum class Namespace {
        PROFILE,
        GLOBAL,
        ;

        fun key(base: String, profileId: Long): String {
            return when (this) {
                PROFILE -> namespacedKey(base, profileId)
                GLOBAL -> base
            }
        }

        fun matches(key: String, profileId: Long): Boolean {
            return when (this) {
                PROFILE -> key.startsWith(profilePrefix(profileId)) ||
                    key.startsWith(appStateProfilePrefix(profileId)) ||
                    key.startsWith(privateProfilePrefix(profileId))
                GLOBAL -> true
            }
        }

        fun stripPrefix(key: String, profileId: Long): String {
            return when {
                key.startsWith(profilePrefix(profileId)) -> key.removePrefix(profilePrefix(profileId))
                key.startsWith(appStateProfilePrefix(profileId)) -> Preference.appStateKey(
                    key.removePrefix(appStateProfilePrefix(profileId)),
                )
                key.startsWith(privateProfilePrefix(profileId)) -> Preference.privateKey(
                    key.removePrefix(privateProfilePrefix(profileId)),
                )
                else -> key
            }
        }

        companion object {
            fun isNamespacedKey(key: String): Boolean {
                return hasNamespacedPrefix(key, "profile_") ||
                    hasNamespacedPrefix(key, Preference.appStateKey("profile_")) ||
                    hasNamespacedPrefix(key, Preference.privateKey("profile_"))
            }

            fun namespacedKey(base: String, profileId: Long): String {
                return when {
                    Preference.isAppState(base) -> Preference.appStateKey(
                        "profile_$profileId:${Preference.stripAppStateKey(base)}",
                    )
                    Preference.isPrivate(base) -> Preference.privateKey(
                        "profile_$profileId:${Preference.stripPrivateKey(base)}",
                    )
                    else -> profilePrefix(profileId) + base
                }
            }

            private fun profilePrefix(profileId: Long) = "profile_$profileId:"

            private fun appStateProfilePrefix(profileId: Long) = Preference.appStateKey("profile_$profileId:")

            private fun privateProfilePrefix(profileId: Long) = Preference.privateKey("profile_$profileId:")

            private fun hasNamespacedPrefix(key: String, prefix: String): Boolean {
                if (!key.startsWith(prefix)) return false

                val remainder = key.removePrefix(prefix)
                val idPart = remainder.substringBefore(':', missingDelimiterValue = "")
                return idPart.isNotEmpty() && idPart.all { it.isDigit() } && remainder.getOrNull(idPart.length) == ':'
            }
        }
    }
}

private class DynamicPreference<T>(
    private val preferences: SharedPreferences,
    private val keyFlow: Flow<String?>,
    private val profileFlow: Flow<Long>,
    private val defaultValue: T,
    private val keyProvider: () -> String,
    private val read: (SharedPreferences, String, T) -> T,
    private val write: (SharedPreferences.Editor, String, T) -> Unit,
) : Preference<T> {

    override fun key(): String = keyProvider()

    override fun get(): T {
        return try {
            read(preferences, key(), defaultValue)
        } catch (_: ClassCastException) {
            delete()
            defaultValue
        }
    }

    override fun set(value: T) {
        val key = key()
        preferences.edit(commit = true) {
            write(this, key, value)
        }
    }

    override fun isSet(): Boolean = preferences.contains(key())

    override fun delete() {
        preferences.edit(commit = true) {
            remove(key())
        }
    }

    override fun defaultValue(): T = defaultValue

    override fun changes(): Flow<T> {
        return merge(
            profileFlow.map { key() },
            keyFlow.filter { changedKey -> changedKey == null || changedKey == key() }.map { key() },
        )
            .onStart { emit(key()) }
            .map { get() }
            .conflate()
    }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> {
        return changes().stateIn(scope, SharingStarted.Eagerly, get())
    }
}
