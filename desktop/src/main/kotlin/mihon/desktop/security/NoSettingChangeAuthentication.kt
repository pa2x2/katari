package mihon.desktop.security

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication
import mihon.core.platform.PlatformContext

/**
 * Desktop cannot ask the user to authenticate yet, so these changes go through unasked, as they do on an Android device
 * without a screen lock. Stands in until desktop authenticates through the system.
 */
@Inject
class NoSettingChangeAuthentication : SettingChangeAuthentication {
    override suspend fun authenticate(context: PlatformContext, title: String): Boolean = true
}
