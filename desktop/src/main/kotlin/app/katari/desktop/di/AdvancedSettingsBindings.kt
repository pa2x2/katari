package app.katari.desktop.di

import app.katari.desktop.advanced.NoBackgroundWorkRestrictions
import app.katari.desktop.advanced.NoExtensionInstallerSetting
import app.katari.desktop.advanced.NoHardwareBitmapSetting
import app.katari.desktop.debug.DesktopDeviceDiagnostics
import app.katari.desktop.notification.DesktopAppNotifications
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.advanced.BackgroundWorkRestrictions
import eu.kanade.presentation.more.settings.screen.advanced.ExtensionInstallerSetting
import eu.kanade.presentation.more.settings.screen.advanced.HardwareBitmapSetting
import eu.kanade.presentation.more.settings.screen.debug.DeviceDiagnostics
import eu.kanade.presentation.more.settings.screen.notifications.AppNotifications

@BindingContainer
interface AdvancedSettingsBindings {
    @Binds
    val DesktopAppNotifications.appNotifications: AppNotifications

    @Binds
    val NoBackgroundWorkRestrictions.backgroundWorkRestrictions: BackgroundWorkRestrictions

    @Binds
    val NoHardwareBitmapSetting.hardwareBitmapSetting: HardwareBitmapSetting

    @Binds
    val NoExtensionInstallerSetting.extensionInstallerSetting: ExtensionInstallerSetting

    @Binds
    val DesktopDeviceDiagnostics.deviceDiagnostics: DeviceDiagnostics
}
