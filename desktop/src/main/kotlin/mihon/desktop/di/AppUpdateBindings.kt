package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings
import mihon.desktop.update.DesktopAppUpdatePlatform
import mihon.desktop.update.DesktopAppUpdateSettings
import mihon.feature.appupdate.AppUpdatePlatform

@BindingContainer
interface AppUpdateBindings {
    @Binds
    val DesktopAppUpdatePlatform.appUpdatePlatform: AppUpdatePlatform

    @Binds
    val DesktopAppUpdateSettings.appUpdateSettings: AppUpdateSettings
}
