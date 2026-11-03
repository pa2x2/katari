package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.more.settings.screen.about.OpenSourceLibraryDefinitions
import eu.kanade.tachiyomi.BuildConfig
import mihon.app.ui.buildinfo.AppBuild
import mihon.app.ui.buildinfo.AppBuildType
import mihon.desktop.about.DesktopOpenSourceLibraryDefinitions
import mihon.desktop.application.DesktopAppVariant
import mihon.desktop.application.DesktopBuildInfo
import mihon.desktop.release.DesktopRelease

@BindingContainer
interface AboutBindings {
    @Binds
    val DesktopOpenSourceLibraryDefinitions.openSourceLibraryDefinitions: OpenSourceLibraryDefinitions

    companion object {
        // Firebase isn't built into desktop builds.
        @Provides
        fun appBuild(): AppBuild {
            val release = DesktopAppVariant.current == DesktopAppVariant.RELEASE
            return AppBuild(
                type = if (release) AppBuildType.Stable else AppBuildType.Debug,
                versionName = DesktopBuildInfo.VERSION_NAME,
                // The app-upgrade migrations are numbered by the Android app's releases, and run on desktop as the
                // code shared with Android at this commit has them, whatever the desktop app's own version.
                versionCode = BuildConfig.VERSION_CODE,
                commitSha = DesktopBuildInfo.COMMIT_SHA,
                commitCount = DesktopBuildInfo.COMMIT_COUNT,
                buildTime = DesktopBuildInfo.BUILD_TIME,
                telemetryIncluded = false,
                releaseNotesUrl = if (release) DesktopRelease.pageUrl else null,
                hidesUnfinishedFeatures = release,
            )
        }
    }
}
