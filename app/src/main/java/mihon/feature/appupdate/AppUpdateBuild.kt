package mihon.feature.appupdate

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.isFossBuildType
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import eu.kanade.tachiyomi.util.system.updaterEnabled

/** What the updater needs to know about the running build. */
internal object AppUpdateBuild {

    const val REPOSITORY = "pa2x2/katari"

    /**
     * Only published build types update themselves: debug, preview and benchmark builds install under other
     * application IDs, which no released APK can replace.
     */
    val isSupported: Boolean get() = updaterEnabled && (isReleaseBuildType || isFossBuildType)

    val isFoss: Boolean get() = isFossBuildType

    val versionName: String get() = BuildConfig.VERSION_NAME

    /** The GitHub release of the running version. */
    val releasePageUrl: String get() = "https://github.com/$REPOSITORY/releases/tag/v${BuildConfig.VERSION_NAME}"
}
