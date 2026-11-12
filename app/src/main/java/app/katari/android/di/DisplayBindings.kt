package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AndroidInterfaceSizeSetting
import eu.kanade.presentation.more.settings.screen.appearance.InterfaceSizeSetting
import eu.kanade.presentation.more.settings.screen.touchpad.AndroidTouchpadSettings
import eu.kanade.presentation.more.settings.screen.touchpad.TouchpadSettings

@BindingContainer
interface DisplayBindings {
    @Binds
    val AndroidInterfaceSizeSetting.interfaceSizeSetting: InterfaceSizeSetting

    @Binds
    val AndroidTouchpadSettings.touchpadSettings: TouchpadSettings
}
