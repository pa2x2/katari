package app.katari.ui.buildinfo

/**
 * The running build as the settings describe it: About and the debug info name its version. Each platform's graph fills
 * it from that platform's build.
 */
data class AppBuild(
    val type: AppBuildType,
    val versionName: String,
    /** The number each release counts up, which tells the app-upgrade migrations which version the app last ran as. */
    val versionCode: Int,
    /** The short hash of the commit the build was made from. */
    val commitSha: String,
    /** How many commits lead up to the build's, which numbers preview builds. */
    val commitCount: String,
    /** When the build was made, as an ISO 8601 instant; shown as it is where it doesn't parse. */
    val buildTime: String,
    /** The page with the running version's release notes; null where there is none, such as for debug builds. */
    val releaseNotesUrl: String?,
    /** Whether features not ready for stable releases, such as setting update intervals by hand, are hidden. */
    val hidesUnfinishedFeatures: Boolean,
)

/** How About names a build's version. */
enum class AppBuildType {
    Debug,
    Preview,

    /** A tagged release; whether it is stable or a pre-release comes from [AppBuild.versionName]. */
    Release,
}
