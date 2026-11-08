package tachiyomi.core.common.preference

import mihon.core.common.platform.PlatformFacility

/**
 * The preference files each platform keeps beside the profiles' stores, opened by what they hold. Each platform names
 * and places its files itself, so a file an earlier version wrote is read where it was written.
 */
@PlatformFacility
fun interface PlatformPreferenceFiles {
    fun open(file: PlatformPreferenceFile): PreferenceStore
}

enum class PlatformPreferenceFile {
    /**
     * The store preference classes read when they name none, in the file the base store reads too. It doesn't come
     * from the profile store, which on Android reads its own preferences from it.
     */
    DEFAULT,

    /** Tracker updates queued by versions that had trackers, which upgrading empties. */
    LEGACY_TRACKING_QUEUE,

    /** The manga chapters queued for download. */
    MANGA_DOWNLOAD_QUEUE,

    /** Downloaded manga chapters marked for deletion, which the reader deletes when it closes. */
    MANGA_CHAPTERS_TO_DELETE,

    /** The anime episodes queued for download. */
    ANIME_DOWNLOAD_QUEUE,

    /** Book downloads queued by versions that kept the queue in preferences, before its own file. */
    LEGACY_BOOK_DOWNLOAD_QUEUE,
}
