package mihon.desktop.storage

import dev.zacsweers.metro.Inject
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.core.common.preference.PlatformPreferenceFile
import tachiyomi.core.common.preference.PlatformPreferenceFiles
import tachiyomi.core.common.preference.PreferenceStore

@Inject
class DesktopPreferenceFiles(
    private val directories: DesktopDirectories,
    private val profileStore: ProfileStore,
) : PlatformPreferenceFiles {
    override fun open(file: PlatformPreferenceFile): PreferenceStore = when (file) {
        // The profile store's own instance, as two stores of one file would overwrite each other's writes
        PlatformPreferenceFile.DEFAULT -> profileStore.basePreferenceStore()
        // In the data directory, since it holds progress not yet sent anywhere else.
        PlatformPreferenceFile.TRACKING_QUEUE -> DesktopPreferenceStore(directories.data.resolve("tracking-queue.json"))
    }
}
