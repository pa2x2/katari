package eu.kanade.tachiyomi.ui.security

import android.os.Bundle
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import logcat.LogPriority
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Blank activity with a BiometricPrompt.
 */
class UnlockActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BiometricAuthentication.isAuthenticating) return

        launchAuthentication(
            BiometricAuthentication.promptInfo(
                title = stringResource(MR.strings.unlock_app_title, stringResource(MR.strings.app_name)),
                confirmationRequired = false,
            ),
        )
    }

    override fun onUnclaimedAuthenticationResult(result: BiometricAuthenticationResult) {
        when (result) {
            BiometricAuthenticationResult.Success -> {
                val profileManager = Injekt.get<ProfileManager>()
                profileManager.markProfileAuthenticated(profileManager.activeProfileId)
                SecureActivityDelegate.unlock()
                finish()
            }
            is BiometricAuthenticationResult.Error -> {
                logcat(LogPriority.ERROR) { result.message.toString() }
                finishAffinity()
            }
        }
    }
}
