package app.katari.desktop.di

import app.katari.desktop.advanced.DesktopAppNotifications
import app.katari.desktop.advanced.NoBackgroundWorkRestrictions
import app.katari.desktop.advanced.NoExtensionInstallerSetting
import app.katari.desktop.advanced.NoHardwareBitmapSetting
import app.katari.desktop.debug.DesktopDeviceDiagnostics
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.advanced.AppNotifications
import eu.kanade.presentation.more.settings.screen.advanced.BackgroundWorkRestrictions
import eu.kanade.presentation.more.settings.screen.advanced.ExtensionInstallerSetting
import eu.kanade.presentation.more.settings.screen.advanced.HardwareBitmapSetting
import eu.kanade.presentation.more.settings.screen.debug.DeviceDiagnostics

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
