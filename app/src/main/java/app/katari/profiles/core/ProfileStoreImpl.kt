package app.katari.profiles.core

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class ProfileStoreImpl(
    private val application: Application,
    private val profilesPreferences: ProfilesPreferences,
) : ProfileAwareStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val baseStore = AndroidPreferenceStore(
        application,
        PreferenceManager.getDefaultSharedPreferences(application),
    )
    private val currentProfileState = MutableStateFlow(profilesPreferences.activeProfileId.get())

    private val profilePreferenceStore = ProfileAwarePreferenceStore(
        backing = baseStore,
        profileProvider = ::currentProfileId,
        profileFlow = currentProfileState,
        namespace = ProfileAwarePreferenceStore.Namespace.PROFILE,
    )
    private val appStatePreferenceStore = ProfileAwarePreferenceStore(
        backing = baseStore,
        profileProvider = ::currentProfileId,
        profileFlow = currentProfileState,
        namespace = ProfileAwarePreferenceStore.Namespace.PROFILE,
    )
    private val privatePreferenceStore = ProfileAwarePreferenceStore(
        backing = baseStore,
        profileProvider = ::currentProfileId,
        profileFlow = currentProfileState,
        namespace = ProfileAwarePreferenceStore.Namespace.PROFILE,
    )

    init {
        profilesPreferences.activeProfileId.changes()
            .onEach { currentProfileState.value = it }
            .launchIn(scope)
    }

    override val currentProfileId: Long
        get() = currentProfileState.value

    override val activeProfileId: Long
        get() = currentProfileId

    override val currentProfileIdFlow: Flow<Long> = currentProfileState.asStateFlow()

    override val activeProfileIdFlow: Flow<Long>
        get() = currentProfileIdFlow

    override fun setCurrentProfileId(profileId: Long) {
        profilesPreferences.activeProfileId.set(profileId)
        currentProfileState.value = profileId
    }

    override fun basePreferenceStore(): PreferenceStore = baseStore

    override fun appStateStore(): PreferenceStore = appStatePreferenceStore

    override fun appStateStore(profileId: Long): PreferenceStore {
        return ProfileAwarePreferenceStore(
            backing = baseStore,
            profileProvider = { profileId },
            profileFlow = currentProfileState,
            namespace = ProfileAwarePreferenceStore.Namespace.PROFILE,
        )
    }

    override fun privateStore(): PreferenceStore = privatePreferenceStore

    override fun privateStore(profileId: Long): PreferenceStore {
        return ProfileAwarePreferenceStore(
            backing = baseStore,
            profileProvider = { profileId },
            profileFlow = currentProfileState,
            namespace = ProfileAwarePreferenceStore.Namespace.PROFILE,
        )
    }

    override fun profileStore(): PreferenceStore = profilePreferenceStore

    override fun profileStore(profileId: Long): PreferenceStore {
        return ProfileAwarePreferenceStore(
            backing = baseStore,
            profileProvider = { profileId },
            profileFlow = currentProfileState,
            namespace = ProfileAwarePreferenceStore.Namespace.PROFILE,
        )
    }

    // Android keeps every profile's preferences in the base file, under keys that carry the profile.
    override fun isProfilePreference(preference: Preference<*>): Boolean {
        val key = preference.key()
        return ProfileAwarePreferenceStore.Namespace.isNamespacedKey(key) ||
            key.startsWith(Preference.appStateKey("profile_")) ||
            key.startsWith(Preference.privateKey("profile_"))
    }

    override fun preferenceKey(preference: Preference<*>): String =
        ProfileAwarePreferenceStore.Namespace.preferenceWrite(preference.key()).key

    // The shared store's file holds every profile's preferences too, so its listener hears them all. Shared
    // preferences hold listeners weakly; the handle keeps this one.
    override fun watchPreferenceWrites(listener: (PreferenceWrite) -> Unit): AutoCloseable {
        val preferences = baseStore.sharedPreferences
        val keyListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null) listener(ProfileAwarePreferenceStore.Namespace.preferenceWrite(key))
        }
        preferences.registerOnSharedPreferenceChangeListener(keyListener)
        return AutoCloseable { preferences.unregisterOnSharedPreferenceChangeListener(keyListener) }
    }

    fun profileKeys(profileId: Long): Set<String> {
        val profileStore = profileStore(profileId) as ProfileAwarePreferenceStore
        val appStateStore = appStateStore(profileId) as ProfileAwarePreferenceStore
        val privateStore = privateStore(profileId) as ProfileAwarePreferenceStore
        return buildSet {
            addAll(profileStore.keys(profileId))
            addAll(appStateStore.keys(profileId))
            addAll(privateStore.keys(profileId))
        }
    }

    fun deleteKeys(keys: Set<String>) {
        if (keys.isEmpty()) return
        baseStore.sharedPreferences.edit(commit = true) {
            keys.forEach(::remove)
        }
    }

    override fun deleteProfileState(profileId: Long) {
        deleteKeys(profileKeys(profileId))
    }
}
