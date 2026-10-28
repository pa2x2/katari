package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.security.AndroidSettingChangeAuthentication
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication

@BindingContainer
interface AuthenticationBindings {
    @Binds
    val AndroidSettingChangeAuthentication.settingChangeAuthentication: SettingChangeAuthentication
}
