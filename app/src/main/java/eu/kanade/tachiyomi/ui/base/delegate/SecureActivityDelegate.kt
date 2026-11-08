package eu.kanade.tachiyomi.ui.base.delegate

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.ui.security.BiometricAuthentication
import eu.kanade.tachiyomi.ui.security.UnlockActivity
import eu.kanade.tachiyomi.util.system.isAuthenticationSupported
import eu.kanade.tachiyomi.util.view.setSecureScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.app.di.appGraph

interface SecureActivityDelegate {
    /**
     * Puts [activity] behind the app lock. The lock's prompt waits while [appUnlockAllowed] is false, for an activity
     * that first decides itself what to unlock.
     */
    fun registerSecureActivity(activity: AppCompatActivity, appUnlockAllowed: () -> Boolean = { true })

    /** Asks for the app unlock when the app is locked and the registered activity allows it. */
    fun requestAppUnlock()

    companion object {
        /**
         * Set to true if we need the first activity to authenticate.
         *
         * Always require unlock if app is killed.
         */
        var requireUnlock = true

        fun onApplicationStopped(preferences: SecurityPreferences) {
            if (!preferences.useAuthenticator.get()) return

            if (!BiometricAuthentication.isAuthenticating) {
                // Return if app is closed in locked state
                if (requireUnlock) return
                // Save app close time if lock is delayed
                if (preferences.lockAppAfter.get() > 0) {
                    preferences.lastAppClosed.set(System.currentTimeMillis())
                }
            }
        }

        /**
         * Checks if unlock is needed when app comes foreground.
         */
        fun onApplicationStart(preferences: SecurityPreferences) {
            if (!preferences.useAuthenticator.get()) return

            val lastClosedPref = preferences.lastAppClosed

            // `requireUnlock` can be true on process start or if app was closed in locked state
            if (!BiometricAuthentication.isAuthenticating && !requireUnlock) {
                requireUnlock = when (val lockDelay = preferences.lockAppAfter.get()) {
                    -1 -> false // Never
                    0 -> true // Always
                    else -> lastClosedPref.get() + lockDelay * 60_000 <= System.currentTimeMillis()
                }
            }

            lastClosedPref.delete()
        }

        fun unlock() {
            requireUnlock = false
        }
    }
}

class SecureActivityDelegateImpl : SecureActivityDelegate, DefaultLifecycleObserver {

    private lateinit var activity: AppCompatActivity
    private lateinit var appUnlockAllowed: () -> Boolean

    private val preferences by lazy { activity.appGraph.basePreferences }
    private val securityPreferences by lazy { activity.appGraph.securityPreferences }

    override fun registerSecureActivity(activity: AppCompatActivity, appUnlockAllowed: () -> Boolean) {
        this.activity = activity
        this.appUnlockAllowed = appUnlockAllowed
        activity.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        setSecureScreen()
    }

    override fun onResume(owner: LifecycleOwner) {
        requestAppUnlock()
    }

    override fun requestAppUnlock() {
        if (!securityPreferences.useAuthenticator.get()) return
        if (activity.isAuthenticationSupported()) {
            if (!SecureActivityDelegate.requireUnlock) return
            if (BiometricAuthentication.isAuthenticating) return
            if (!appUnlockAllowed()) return
            activity.startActivity(Intent(activity, UnlockActivity::class.java))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }
        } else {
            securityPreferences.useAuthenticator.set(false)
        }
    }

    private fun setSecureScreen() {
        val secureScreenFlow = securityPreferences.secureScreen.changes()
        val incognitoModeFlow = preferences.incognitoMode.changes()
        combine(secureScreenFlow, incognitoModeFlow) { secureScreen, incognitoMode ->
            secureScreen == SecurityPreferences.SecureScreenMode.ALWAYS ||
                (secureScreen == SecurityPreferences.SecureScreenMode.INCOGNITO && incognitoMode)
        }
            .onEach(activity.window::setSecureScreen)
            .launchIn(activity.lifecycleScope)
    }
}
