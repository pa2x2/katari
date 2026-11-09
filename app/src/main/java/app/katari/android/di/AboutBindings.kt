package app.katari.android.di

import app.katari.appupdate.AppUpdateBuild
import app.katari.appupdate.ui.AndroidAppUpdateSettings
import app.katari.ui.buildinfo.AppBuild
import app.katari.ui.buildinfo.AppBuildType
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.more.settings.screen.about.AndroidOpenSourceLibraryDefinitions
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings
import eu.kanade.presentation.more.settings.screen.about.OpenSourceLibraryDefinitions
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import eu.kanade.tachiyomi.util.system.isReleaseBuildType

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
                else -> AppBuildType.Release
            },
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            commitSha = BuildConfig.COMMIT_SHA,
            commitCount = BuildConfig.COMMIT_COUNT,
            buildTime = BuildConfig.BUILD_TIME,
            releaseNotesUrl = if (BuildConfig.DEBUG) null else AppUpdateBuild.releasePageUrl,
            hidesUnfinishedFeatures = isReleaseBuildType,
        )
    }
}
