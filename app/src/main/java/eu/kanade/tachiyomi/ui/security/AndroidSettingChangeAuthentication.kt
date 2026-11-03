package eu.kanade.tachiyomi.ui.security

import androidx.fragment.app.FragmentActivity
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.security.BiometricAuthentication.authenticate
import mihon.core.platform.PlatformContext

/** Asks with the device's own unlock, prompted from the settings activity. */
@Inject
class AndroidSettingChangeAuthentication : SettingChangeAuthentication {
    override suspend fun authenticate(context: PlatformContext, title: String): Boolean =
        (context as FragmentActivity).authenticate(title = title)
}
