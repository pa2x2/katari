package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.desktop.security.DesktopSettingChangeAuthentication
import app.katari.desktop.security.NoSecureScreen
import app.katari.desktop.security.signin.SystemSignInCheck
import app.katari.desktop.security.signin.systemSignInCheck
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import eu.kanade.presentation.more.settings.screen.security.SecureScreen
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication

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
