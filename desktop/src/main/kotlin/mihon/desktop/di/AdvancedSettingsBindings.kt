package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.advanced.AppNotifications
import eu.kanade.presentation.more.settings.screen.advanced.BackgroundWorkRestrictions
import eu.kanade.presentation.more.settings.screen.advanced.ExtensionInstallerSetting
import eu.kanade.presentation.more.settings.screen.advanced.ReaderDecodingSettings
import eu.kanade.presentation.more.settings.screen.debug.DeviceDiagnostics
import mihon.desktop.advanced.NoAppNotifications
import mihon.desktop.advanced.NoBackgroundWorkRestrictions
import mihon.desktop.advanced.NoExtensionInstallerSetting
import mihon.desktop.advanced.NoReaderDecodingSettings
import mihon.desktop.debug.DesktopDeviceDiagnostics

@BindingContainer
interface AdvancedSettingsBindings {
    @Binds
    val NoAppNotifications.appNotifications: AppNotifications

    @Binds
    val NoBackgroundWorkRestrictions.backgroundWorkRestrictions: BackgroundWorkRestrictions

    @Binds
    val NoReaderDecodingSettings.readerDecodingSettings: ReaderDecodingSettings

    @Binds
    val NoExtensionInstallerSetting.extensionInstallerSetting: ExtensionInstallerSetting

    @Binds
    val DesktopDeviceDiagnostics.deviceDiagnostics: DeviceDiagnostics
}
