package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.more.settings.screen.security.SecureScreen
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication
import mihon.core.common.platform.DesktopOperatingSystem
import mihon.desktop.security.DesktopSettingChangeAuthentication
import mihon.desktop.security.NoSecureScreen
import mihon.desktop.security.signin.SystemSignInCheck
import mihon.desktop.security.signin.systemSignInCheck

@BindingContainer
interface AuthenticationBindings {
    @Binds
    val DesktopSettingChangeAuthentication.settingChangeAuthentication: SettingChangeAuthentication

    @Binds
    val NoSecureScreen.secureScreen: SecureScreen

    companion object {
        @Provides
        fun signInCheck(operatingSystem: DesktopOperatingSystem): SystemSignInCheck =
            systemSignInCheck(operatingSystem)
    }
}
