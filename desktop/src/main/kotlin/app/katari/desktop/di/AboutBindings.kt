package app.katari.desktop.di

import app.katari.desktop.about.DesktopOpenSourceLibraryDefinitions
import app.katari.desktop.application.DesktopAppVariant
import app.katari.desktop.application.DesktopBuildInfo
import app.katari.desktop.release.DesktopRelease
import app.katari.ui.buildinfo.AppBuild
import app.katari.ui.buildinfo.AppBuildType
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.more.settings.screen.about.OpenSourceLibraryDefinitions
import eu.kanade.tachiyomi.BuildConfig

@BindingContainer
interface AboutBindings {
    @Binds
    val DesktopOpenSourceLibraryDefinitions.openSourceLibraryDefinitions: OpenSourceLibraryDefinitions

    companion object {
        @Provides
        fun appBuild(): AppBuild {
            val release = DesktopAppVariant.current == DesktopAppVariant.RELEASE
            return AppBuild(
                type = if (release) AppBuildType.Release else AppBuildType.Debug,
                versionName = DesktopBuildInfo.VERSION_NAME,
                // The app-upgrade migrations are numbered by the Android app's releases, and run on desktop as the
                // code shared with Android at this commit has them, whatever the desktop app's own version.
                versionCode = BuildConfig.VERSION_CODE,
                commitSha = DesktopBuildInfo.COMMIT_SHA,
                commitCount = DesktopBuildInfo.COMMIT_COUNT,
                buildTime = DesktopBuildInfo.BUILD_TIME,
                releaseNotesUrl = if (release) DesktopRelease.pageUrl else null,
                hidesUnfinishedFeatures = release,
            )
        }
    }
}
