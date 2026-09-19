package mihon.core.migration.migrations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Preference-store fixtures shared by the profile-scoped migration tests in this package.
 */
internal class MigrationTestProfileStore(
    private val stores: Map<Long, PreferenceStore>,
) : ProfileStore {
    override val currentProfileId: Long = stores.keys.first()
    override val currentProfileIdFlow: Flow<Long> = flowOf(currentProfileId)
    override fun setCurrentProfileId(profileId: Long) = Unit
    override fun basePreferenceStore(): PreferenceStore = stores.getValue(currentProfileId)
    override fun appStateStore(): PreferenceStore = stores.getValue(currentProfileId)
    override fun privateStore(): PreferenceStore = stores.getValue(currentProfileId)
    override fun profileStore(): PreferenceStore = stores.getValue(currentProfileId)
    override fun profileStore(profileId: Long): PreferenceStore = stores.getValue(profileId)
    override fun appStateStore(profileId: Long): PreferenceStore = stores.getValue(profileId)
    override fun privateStore(profileId: Long): PreferenceStore = stores.getValue(profileId)
    override fun sourcePreferenceKey(sourceId: Long, profileId: Long): String = "source_${profileId}_$sourceId"
}

internal class MigrationTestPreferenceStore : PreferenceStore {
    private val values = mutableMapOf<String, MigrationTestPreference<*>>()

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
        return values.getOrPut(key) { MigrationTestPreference(key, defaultValue) } as Preference<T>
    }
}

internal class MigrationTestPreference<T>(
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
