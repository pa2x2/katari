package mihon.desktop.profile

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.LogPriority
import mihon.desktop.storage.DesktopDirectories
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.core.ProfilesPreferences
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * The preference stores Android keeps in one file with profile-prefixed keys, as one file each in the configuration
 * directory: `base.json` for what all profiles share, and `profiles/<id>/profile.json`, `app-state.json` and
 * `private.json` for each profile. Every file has a single store that all callers share, since two stores on one file
 * would each overwrite the other's writes.
 *
 * The active profile is remembered in the base store under the key Android uses. The stores asked for without a
 * profile ID follow it, as Android's do, so preference classes created once read and write whichever profile is active.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopProfileStore(private val directories: DesktopDirectories) : ProfileStore, ActiveProfileProvider {

    private val stores = ConcurrentHashMap<Path, DesktopPreferenceStore>()
    private val profilesPreferences = ProfilesPreferences(basePreferenceStore())
    private val activeProfile = MutableStateFlow(profilesPreferences.activeProfileId.get())

    private val activeProfileStore = ActiveProfilePreferenceStore(activeProfile, ::profileStore)
    private val activeAppStateStore = ActiveProfilePreferenceStore(activeProfile, ::appStateStore)
    private val activePrivateStore = ActiveProfilePreferenceStore(activeProfile, ::privateStore)

    override val currentProfileId: Long
        get() = activeProfile.value

    override val currentProfileIdFlow: Flow<Long> = activeProfile.asStateFlow()

    override val activeProfileId: Long
        get() = currentProfileId

    override val activeProfileIdFlow: Flow<Long>
        get() = currentProfileIdFlow

    override fun setCurrentProfileId(profileId: Long) {
        profilesPreferences.activeProfileId.set(profileId)
        activeProfile.value = profileId
    }

    override fun basePreferenceStore(): PreferenceStore = store(directories.configuration.resolve("base.json"))

    override fun profileStore(): PreferenceStore = activeProfileStore

    override fun profileStore(profileId: Long): PreferenceStore = store(profileFile(profileId, PROFILE_FILE))

    override fun appStateStore(): PreferenceStore = activeAppStateStore

    override fun appStateStore(profileId: Long): PreferenceStore = store(profileFile(profileId, APP_STATE_FILE))

    override fun privateStore(): PreferenceStore = activePrivateStore

    override fun privateStore(profileId: Long): PreferenceStore = store(profileFile(profileId, PRIVATE_FILE))

    override fun isProfilePreference(preference: Preference<*>): Boolean =
        listOf(activeProfileStore, activeAppStateStore, activePrivateStore).any { it.owns(preference) } ||
            stores.any { (path, store) -> path.startsWith(profilesDirectory()) && store.owns(preference) }

    // The stores stay open, empty, for a new profile that gets the same ID.
    override fun deleteProfileState(profileId: Long) {
        PROFILE_FILES.forEach { name -> store(profileFile(profileId, name)).clear() }
        val directory = profileDirectory(profileId)
        try {
            Files.deleteIfExists(directory)
        } catch (_: DirectoryNotEmptyException) {
            // Something besides the preferences lives there; it belongs to whoever put it there.
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Could not delete $directory" }
        }
    }

    private fun profilesDirectory(): Path = directories.configuration.resolve("profiles")

    private fun profileDirectory(profileId: Long): Path = profilesDirectory().resolve(profileId.toString())

    private fun profileFile(profileId: Long, name: String): Path = profileDirectory(profileId).resolve(name)

    private fun store(path: Path): DesktopPreferenceStore = stores.computeIfAbsent(path, ::DesktopPreferenceStore)

    private companion object {
        const val PROFILE_FILE = "profile.json"
        const val APP_STATE_FILE = "app-state.json"
        const val PRIVATE_FILE = "private.json"
        val PROFILE_FILES = listOf(PROFILE_FILE, APP_STATE_FILE, PRIVATE_FILE)
    }
}
