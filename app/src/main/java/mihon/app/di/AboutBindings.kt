package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.more.settings.screen.about.AndroidOpenSourceLibraryDefinitions
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings
import eu.kanade.presentation.more.settings.screen.about.OpenSourceLibraryDefinitions
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import eu.kanade.tachiyomi.util.system.telemetryIncluded
import mihon.app.ui.buildinfo.AppBuild
import mihon.app.ui.buildinfo.AppBuildType
import mihon.feature.appupdate.AppUpdateBuild
import mihon.feature.appupdate.ui.AndroidAppUpdateSettings

@BindingContainer
interface AboutBindings {
    @Binds
    val AndroidAppUpdateSettings.appUpdateSettings: AppUpdateSettings

    @Binds
    val AndroidOpenSourceLibraryDefinitions.openSourceLibraryDefinitions: OpenSourceLibraryDefinitions

    companion object {
        @Provides
        fun appBuild(): AppBuild = AppBuild(
            type = when {
                BuildConfig.DEBUG -> AppBuildType.Debug
                isPreviewBuildType -> AppBuildType.Preview
                else -> AppBuildType.Stable
            },
            versionName = BuildConfig.VERSION_NAME,
            commitSha = BuildConfig.COMMIT_SHA,
            commitCount = BuildConfig.COMMIT_COUNT,
            buildTime = BuildConfig.BUILD_TIME,
            telemetryIncluded = telemetryIncluded,
            releaseNotesUrl = if (BuildConfig.DEBUG) null else AppUpdateBuild.releasePageUrl,
        )
    }
}
