package eu.kanade.tachiyomi.ui.security

import androidx.fragment.app.FragmentActivity
import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.security.BiometricAuthentication.authenticate

/** Asks with the device's own unlock, prompted from the settings activity. */
@Inject
class AndroidSettingChangeAuthentication : SettingChangeAuthentication {
    override suspend fun authenticate(context: PlatformContext, title: String): Boolean =
        (context as FragmentActivity).authenticate(title = title)
}
