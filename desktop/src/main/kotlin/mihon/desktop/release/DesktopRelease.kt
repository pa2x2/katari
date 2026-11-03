package mihon.desktop.release

import mihon.desktop.application.DesktopBuildInfo

/**
 * Where the desktop app is released: as GitHub releases of the app's repository tagged `desktop-v<version>`, beside the
 * Android app's `v<version>` ones, which name the Android app's own versions.
 */
internal object DesktopRelease {
    const val REPOSITORY = "pa2x2/katari"
    const val TAG_PREFIX = "desktop-v"

    /** The GitHub release of the running version. */
    val pageUrl: String
        get() = "https://github.com/$REPOSITORY/releases/tag/$TAG_PREFIX${DesktopBuildInfo.VERSION_NAME}"
}
