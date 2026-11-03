package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.advanced.AndroidDisplayBackendSetting
import eu.kanade.presentation.more.settings.screen.advanced.DisplayBackendSetting
import eu.kanade.presentation.more.settings.screen.appearance.AndroidInterfaceSizeSetting
import eu.kanade.presentation.more.settings.screen.appearance.InterfaceSizeSetting

@BindingContainer
interface DisplayBindings {
    @Binds
    val AndroidInterfaceSizeSetting.interfaceSizeSetting: InterfaceSizeSetting

    @Binds
    val AndroidDisplayBackendSetting.displayBackendSetting: DisplayBackendSetting
}
