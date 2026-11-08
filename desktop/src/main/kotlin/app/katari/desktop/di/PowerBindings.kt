package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.metro.AppCoroutineScope
import app.katari.core.platform.power.PowerSource
import app.katari.core.platform.power.ScreenIdleInhibitor
import app.katari.desktop.power.PortalScreenIdleInhibitor
import app.katari.desktop.power.UPowerPowerSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope

@BindingContainer
object PowerBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun powerSource(
        operatingSystem: DesktopOperatingSystem,
        @AppCoroutineScope scope: CoroutineScope,
    ): PowerSource = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> UPowerPowerSource(scope)
    }

    @Provides
    @SingleIn(AppScope::class)
    fun screenIdleInhibitor(operatingSystem: DesktopOperatingSystem): ScreenIdleInhibitor = when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> PortalScreenIdleInhibitor()
    }
}
