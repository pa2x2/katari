package app.katari.desktop.di

import app.katari.appupdate.AppUpdatePlatform
import app.katari.desktop.update.DesktopAppUpdatePlatform
import app.katari.desktop.update.DesktopAppUpdateSettings
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.about.AppUpdateSettings

@BindingContainer
interface AppUpdateBindings {
    @Binds
    val DesktopAppUpdatePlatform.appUpdatePlatform: AppUpdatePlatform

    @Binds
    val DesktopAppUpdateSettings.appUpdateSettings: AppUpdateSettings
}
