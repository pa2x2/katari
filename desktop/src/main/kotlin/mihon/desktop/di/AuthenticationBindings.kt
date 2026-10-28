package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication
import mihon.desktop.security.NoSettingChangeAuthentication

@BindingContainer
interface AuthenticationBindings {
    @Binds
    val NoSettingChangeAuthentication.settingChangeAuthentication: SettingChangeAuthentication
}
