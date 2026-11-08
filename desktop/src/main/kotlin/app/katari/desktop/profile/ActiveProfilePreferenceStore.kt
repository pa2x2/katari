package app.katari.desktop.profile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * The store of whichever profile is active. Each read and write of its preferences goes to the store of the profile
 * active at that moment, and their changes move to the new profile's value on a switch, so a preference taken from it
 * before a switch never reaches the profile that was left.
 */
internal class ActiveProfilePreferenceStore(
    private val activeProfileId: StateFlow<Long>,
    private val storeOf: (Long) -> PreferenceStore,
) : PreferenceStore {

    override fun getString(key: String, defaultValue: String): Preference<String> =
        preference { it.getString(key, defaultValue) }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> =
        preference { it.getLong(key, defaultValue) }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> = preference { it.getInt(key, defaultValue) }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> =
        preference { it.getFloat(key, defaultValue) }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> =
        preference { it.getBoolean(key, defaultValue) }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> =
        preference { it.getStringSet(key, defaultValue) }

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = preference { it.getObjectFromString(key, defaultValue, serializer, deserializer) }

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> = preference { it.getObjectSetFromStringSet(key, defaultValue, serializer, deserializer) }

    override fun getAll(): Map<String, *> = storeOf(activeProfileId.value).getAll()

    private fun <T> preference(resolve: (PreferenceStore) -> Preference<T>): Preference<T> =
        ActiveProfilePreference(this, activeProfileId) { profileId -> resolve(storeOf(profileId)) }

    /** Whether [preference] was taken from this store. */
    fun owns(preference: Preference<*>): Boolean = preference is ActiveProfilePreference<*> && preference.owner === this
}

private class ActiveProfilePreference<T>(
    val owner: ActiveProfilePreferenceStore,
    private val activeProfileId: StateFlow<Long>,
    private val resolve: (profileId: Long) -> Preference<T>,
) : Preference<T> {

    private fun active(): Preference<T> = resolve(activeProfileId.value)

    override fun key(): String = active().key()

    override fun get(): T = active().get()

    override fun set(value: T) = active().set(value)

    override fun isSet(): Boolean = active().isSet()

    override fun delete() = active().delete()

    override fun defaultValue(): T = active().defaultValue()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun changes(): Flow<T> = activeProfileId.flatMapLatest { profileId -> resolve(profileId).changes() }

    override fun stateIn(scope: CoroutineScope): StateFlow<T> = changes().stateIn(scope, SharingStarted.Eagerly, get())
}
