package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.security.AndroidSecureScreen
import eu.kanade.presentation.more.settings.screen.security.SecureScreen
import eu.kanade.tachiyomi.ui.security.AndroidSettingChangeAuthentication
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication

@BindingContainer
interface AuthenticationBindings {
    @Binds
    val AndroidSettingChangeAuthentication.settingChangeAuthentication: SettingChangeAuthentication

    @Binds
    val AndroidSecureScreen.secureScreen: SecureScreen
}
