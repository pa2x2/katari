package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.app.ui.host.LogDestinations
import mihon.core.platform.PlatformContext
import mihon.desktop.application.ApplicationPlatformContext
import mihon.desktop.application.DesktopAppVariant
import mihon.desktop.application.DesktopBuildInfo
import mihon.desktop.application.installation.AppInstallation
import mihon.desktop.application.installation.systemAppInstallation
import mihon.desktop.logging.DesktopLogDestinations
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.domain.app.AppIdentity

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
