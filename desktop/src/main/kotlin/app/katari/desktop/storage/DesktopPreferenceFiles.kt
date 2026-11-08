package app.katari.desktop.storage

import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore
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
        PlatformPreferenceFile.LEGACY_TRACKING_QUEUE ->
            DesktopPreferenceStore(directories.data.resolve("tracking-queue.json"))
        PlatformPreferenceFile.MANGA_DOWNLOAD_QUEUE ->
            DesktopPreferenceStore(directories.data.resolve("manga-download-queue.json"))
        PlatformPreferenceFile.MANGA_CHAPTERS_TO_DELETE ->
            DesktopPreferenceStore(directories.data.resolve("manga-chapters-to-delete.json"))
        PlatformPreferenceFile.ANIME_DOWNLOAD_QUEUE ->
            DesktopPreferenceStore(directories.data.resolve("anime-download-queue.json"))
        // No desktop version kept the book queue in preferences
        PlatformPreferenceFile.LEGACY_BOOK_DOWNLOAD_QUEUE -> InMemoryPreferenceStore()
    }
}
