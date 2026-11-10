package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.advanced.AndroidBackgroundWorkRestrictions
import eu.kanade.presentation.more.settings.screen.advanced.AndroidExtensionInstallerSetting
import eu.kanade.presentation.more.settings.screen.advanced.AndroidHardwareBitmapSetting
import eu.kanade.presentation.more.settings.screen.advanced.AndroidWebViewData
import eu.kanade.presentation.more.settings.screen.advanced.BackgroundWorkRestrictions
import eu.kanade.presentation.more.settings.screen.advanced.ExtensionInstallerSetting
import eu.kanade.presentation.more.settings.screen.advanced.HardwareBitmapSetting
import eu.kanade.presentation.more.settings.screen.advanced.WebViewData
import eu.kanade.presentation.more.settings.screen.debug.AndroidDeviceDiagnostics
import eu.kanade.presentation.more.settings.screen.debug.DeviceDiagnostics
import eu.kanade.presentation.more.settings.screen.notifications.AndroidAppNotifications
import eu.kanade.presentation.more.settings.screen.notifications.AppNotifications

@BindingContainer
interface AdvancedSettingsBindings {
    @Binds
    val AndroidAppNotifications.appNotifications: AppNotifications

    @Binds
    val AndroidBackgroundWorkRestrictions.backgroundWorkRestrictions: BackgroundWorkRestrictions

    @Binds
    val AndroidWebViewData.webViewData: WebViewData

    @Binds
    val AndroidHardwareBitmapSetting.hardwareBitmapSetting: HardwareBitmapSetting

    @Binds
    val AndroidExtensionInstallerSetting.extensionInstallerSetting: ExtensionInstallerSetting

    @Binds
    val AndroidDeviceDiagnostics.deviceDiagnostics: DeviceDiagnostics
}
