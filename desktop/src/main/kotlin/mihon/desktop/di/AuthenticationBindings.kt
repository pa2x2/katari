package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.security.SecureScreen
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication
import mihon.desktop.security.DesktopSettingChangeAuthentication
import mihon.desktop.security.NoSecureScreen

@BindingContainer
interface AuthenticationBindings {
    @Binds
    val DesktopSettingChangeAuthentication.settingChangeAuthentication: SettingChangeAuthentication

    @Binds
    val NoSecureScreen.secureScreen: SecureScreen
}
