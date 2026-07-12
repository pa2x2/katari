package mihon.feature.profiles.core

import android.app.Application
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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File

class ProfileStoreImpl(
    private val application: Application,
    private val profilesPreferences: ProfilesPreferences = Injekt.get(),
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

    override fun sourcePreferenceKey(sourceId: Long, profileId: Long): String {
        return "source_${profileId}_$sourceId"
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

    fun deleteProfileState(profileId: Long) {
        deleteKeys(profileKeys(profileId))
        deleteSourcePreferences(profileId)
    }

    private fun deleteSourcePreferences(profileId: Long) {
        val prefix = "source_${profileId}_"
        val sharedPrefsDir = File(application.applicationInfo.dataDir, "shared_prefs")
        sharedPrefsDir.listFiles()
            ?.asSequence()
            ?.map { it.name.removeSuffix(".xml") }
            ?.filter { it.startsWith(prefix) }
            ?.forEach { prefName -> application.deleteSharedPreferences(prefName) }
    }
}
