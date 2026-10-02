package mihon.feature.appupdate.check

internal enum class AppUpdateChannel {
    /** Releases flagged as pre-releases on GitHub, or tagged with a pre-release version, are never offered. */
    STABLE,
    PRERELEASE,
}
