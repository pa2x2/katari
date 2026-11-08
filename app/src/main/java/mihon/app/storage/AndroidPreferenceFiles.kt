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
        PlatformPreferenceFile.LEGACY_TRACKING_QUEUE -> named("tracking_queue")
        PlatformPreferenceFile.MANGA_DOWNLOAD_QUEUE -> named("active_downloads")
        PlatformPreferenceFile.MANGA_CHAPTERS_TO_DELETE -> named("chapters_to_delete")
        PlatformPreferenceFile.ANIME_DOWNLOAD_QUEUE -> named("active_anime_downloads")
        PlatformPreferenceFile.LEGACY_BOOK_DOWNLOAD_QUEUE -> named("active_book_downloads")
    }

    private fun named(name: String) =
        AndroidPreferenceStore(application, application.getSharedPreferences(name, Context.MODE_PRIVATE))
}
