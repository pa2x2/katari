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

    /** Tracking updates waiting to be sent. */
    TRACKING_QUEUE,
}
