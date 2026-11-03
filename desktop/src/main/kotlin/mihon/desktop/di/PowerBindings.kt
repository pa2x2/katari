package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.core.platform.power.PowerSource
import mihon.desktop.platform.DesktopOperatingSystem
import mihon.desktop.power.UPowerPowerSource

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
}
