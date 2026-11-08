package mihon.feature.profiles.ui

import androidx.fragment.app.FragmentActivity
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.security.BiometricAuthentication.authenticate
import eu.kanade.tachiyomi.util.system.isAuthenticationSupported
import mihon.core.platform.PlatformContext
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/** Unlocks with the device's own unlock. Without an activity to host the prompt the profiles stay locked. */
@Inject
class AndroidProfileUnlock : ProfileUnlock {
    override fun isAvailable(context: PlatformContext): Boolean = context.isAuthenticationSupported()

    override val lockSettingsWording: ProfileLockWording? = null

    override suspend fun unlock(context: PlatformContext, profileNames: List<String>): Boolean {
        if (profileNames.isEmpty()) return true
        return context is FragmentActivity &&
            context.authenticate(
                title = context.stringResource(MR.strings.unlock_app_title, profileNames.joinToString()),
                subtitle = null,
            )
    }

    override suspend fun unlockToMoveEntries(context: PlatformContext, profileName: String): Boolean =
        context is FragmentActivity &&
            context.authenticate(
                title = context.stringResource(MR.strings.move_entries_auth_title),
                subtitle = context.stringResource(MR.strings.move_entries_auth_subtitle, profileName),
            )
}
