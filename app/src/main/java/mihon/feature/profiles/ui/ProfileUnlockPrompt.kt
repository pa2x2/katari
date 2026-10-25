package mihon.feature.profiles.ui

import android.content.Context
import androidx.fragment.app.FragmentActivity
import eu.kanade.tachiyomi.ui.security.BiometricAuthentication.authenticate
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource

/**
 * Asks once, with the device's unlock, to open the profiles named in [profileNames]; every profile shares that unlock.
 * Without an activity to host the prompt the profiles stay locked.
 */
internal suspend fun unlockProfiles(context: Context, profileNames: List<String>): Boolean {
    if (profileNames.isEmpty()) return true
    return context is FragmentActivity &&
        context.authenticate(
            title = context.stringResource(MR.strings.unlock_app_title, profileNames.joinToString()),
            subtitle = null,
        )
}
