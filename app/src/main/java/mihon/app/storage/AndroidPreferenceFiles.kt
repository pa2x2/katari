package mihon.app.storage

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PlatformPreferenceFile
import tachiyomi.core.common.preference.PlatformPreferenceFiles
import tachiyomi.core.common.preference.PreferenceStore

/** Each file is a shared preferences file of the app's own; the default one is androidx's default file. */
@Inject
class AndroidPreferenceFiles(private val application: Application) : PlatformPreferenceFiles {
    override fun open(file: PlatformPreferenceFile): PreferenceStore = when (file) {
        PlatformPreferenceFile.DEFAULT -> AndroidPreferenceStore(application)
        PlatformPreferenceFile.TRACKING_QUEUE -> AndroidPreferenceStore(
            application,
            application.getSharedPreferences("tracking_queue", Context.MODE_PRIVATE),
        )
    }
}
