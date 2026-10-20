package eu.kanade.tachiyomi.ui.security

import android.os.Bundle
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.base.delegate.SecureActivityDelegate
import logcat.LogPriority
import mihon.app.di.appGraph
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*

/**
 * Blank activity with a BiometricPrompt.
 */
class UnlockActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BiometricAuthentication.isAuthenticating) return

        // The prompt unlocks only the active profile, so name it once there is more than one to tell apart.
        val profileManager = appGraph.profileManager
        val lockedName = profileManager.activeProfile.value?.name
            ?.takeIf { profileManager.visibleProfiles.value.size > 1 }
            ?: stringResource(MR.strings.app_name)
        launchAuthentication(
            BiometricAuthentication.promptInfo(
                title = stringResource(MR.strings.unlock_app_title, lockedName),
                confirmationRequired = false,
            ),
        )
    }

    override fun onUnclaimedAuthenticationResult(result: BiometricAuthenticationResult) {
        when (result) {
            BiometricAuthenticationResult.Success -> {
                val profileManager = appGraph.profileManager
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
