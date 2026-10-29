package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings
import eu.kanade.presentation.more.settings.screen.about.OpenSourceLibraryDefinitions
import mihon.app.ui.buildinfo.AppBuild
import mihon.app.ui.buildinfo.AppBuildType
import mihon.desktop.about.DesktopOpenSourceLibraryDefinitions
import mihon.desktop.about.NoAppUpdateSettings
import mihon.desktop.application.DesktopBuildInfo

@BindingContainer
interface AboutBindings {
    @Binds
    val NoAppUpdateSettings.appUpdateSettings: AppUpdateSettings

    @Binds
    val DesktopOpenSourceLibraryDefinitions.openSourceLibraryDefinitions: OpenSourceLibraryDefinitions

    companion object {
        // Desktop builds come from the working tree and are not released yet, as Android's debug builds; Firebase
        // isn't built into them.
        @Provides
        fun appBuild(): AppBuild = AppBuild(
            type = AppBuildType.Debug,
            versionName = DesktopBuildInfo.VERSION_NAME,
            commitSha = DesktopBuildInfo.COMMIT_SHA,
            commitCount = DesktopBuildInfo.COMMIT_COUNT,
            buildTime = DesktopBuildInfo.BUILD_TIME,
            telemetryIncluded = false,
            releaseNotesUrl = null,
            hidesUnfinishedFeatures = false,
        )
    }
}
