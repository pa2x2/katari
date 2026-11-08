package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.platform.PlatformContext
import app.katari.desktop.application.ApplicationPlatformContext
import app.katari.desktop.application.DesktopAppVariant
import app.katari.desktop.application.DesktopBuildInfo
import app.katari.desktop.application.installation.AppInstallation
import app.katari.desktop.application.installation.systemAppInstallation
import app.katari.desktop.logging.DesktopLogDestinations
import app.katari.domain.app.AppIdentity
import app.katari.ui.host.LogDestinations
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
interface ApplicationBindings {
    @Binds
    val ApplicationPlatformContext.platformContext: PlatformContext

    @Binds
    val DesktopLogDestinations.logDestinations: LogDestinations

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun operatingSystem(): DesktopOperatingSystem = DesktopOperatingSystem.current()

        @Provides
        @SingleIn(AppScope::class)
        fun appInstallation(operatingSystem: DesktopOperatingSystem): AppInstallation =
            systemAppInstallation(operatingSystem)

        @Provides
        fun appIdentity(operatingSystem: DesktopOperatingSystem): AppIdentity = AppIdentity(
            applicationId = DesktopAppVariant.current.applicationId,
            versionName = DesktopBuildInfo.VERSION_NAME,
            commitSha = DesktopBuildInfo.COMMIT_SHA,
            platformName = operatingSystem.displayName,
        )
    }
}
