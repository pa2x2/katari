package mihon.desktop.profile

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.desktop.storage.DesktopDirectories
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.ActiveProfileProvider
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * The preference stores Android keeps in one file with profile-prefixed keys, as one file each in the configuration
 * directory: `base.json` for what all profiles share, and `profiles/<id>/profile.json`, `app-state.json` and
 * `private.json` for each profile. Every file has a single store that all callers share, since two stores on one file
 * would each overwrite the other's writes.
 *
 * Desktop cannot create or switch profiles yet, so the profile the database creates is always the active one.
 */
@Inject
@SingleIn(AppScope::class)
class DesktopProfileStore(private val directories: DesktopDirectories) : ProfileStore, ActiveProfileProvider {

    private val stores = ConcurrentHashMap<Path, DesktopPreferenceStore>()

    override val currentProfileId: Long = ProfileConstants.DEFAULT_PROFILE_ID

    override val currentProfileIdFlow: Flow<Long> = flowOf(currentProfileId)

    override val activeProfileId: Long
        get() = currentProfileId

    override val activeProfileIdFlow: Flow<Long>
        get() = currentProfileIdFlow

    override fun setCurrentProfileId(profileId: Long) {
        check(profileId == currentProfileId) { "Desktop has no profile $profileId; it has only the default profile" }
    }

    override fun basePreferenceStore(): PreferenceStore = store(directories.configuration.resolve("base.json"))

    override fun profileStore(): PreferenceStore = profileStore(currentProfileId)

    override fun profileStore(profileId: Long): PreferenceStore = store(
        profileDirectory(profileId).resolve("profile.json"),
    )

    override fun appStateStore(): PreferenceStore = appStateStore(currentProfileId)

    override fun appStateStore(profileId: Long): PreferenceStore =
        store(profileDirectory(profileId).resolve("app-state.json"))

    override fun privateStore(): PreferenceStore = privateStore(currentProfileId)

    override fun privateStore(profileId: Long): PreferenceStore = store(
        profileDirectory(profileId).resolve("private.json"),
    )

    private fun profileDirectory(profileId: Long): Path =
        directories.configuration.resolve("profiles").resolve(profileId.toString())

    private fun store(path: Path): PreferenceStore = stores.computeIfAbsent(path, ::DesktopPreferenceStore)
}
