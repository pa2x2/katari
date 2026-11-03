package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.security.SecureScreen
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication
import mihon.desktop.security.NoSecureScreen
import mihon.desktop.security.NoSettingChangeAuthentication

@BindingContainer
interface AuthenticationBindings {
    @Binds
    val NoSettingChangeAuthentication.settingChangeAuthentication: SettingChangeAuthentication

    @Binds
    val NoSecureScreen.secureScreen: SecureScreen
}
