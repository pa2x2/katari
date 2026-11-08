package app.katari.showcase.capture.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import app.katari.showcase.capture.ShowcaseDevice.argument
import app.katari.showcase.capture.ShowcaseDevice.enterText
import app.katari.showcase.capture.ShowcaseDevice.find
import app.katari.showcase.capture.ShowcaseDevice.hideKeyboard
import app.katari.showcase.capture.ShowcaseDevice.tap
import app.katari.showcase.capture.ShowcaseDevice.textField
import app.katari.showcase.capture.openProfile
import app.katari.showcase.capture.openSettings
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Signs the phone in to the sync group's account (the `syncServer`, `syncUsername` and `syncPassword` arguments), which
 * takes in its library for the desktop to join. No still: the sync shots come once the desktop has joined.
 */
@RunWith(AndroidJUnit4::class)
class SignInScenes {
    @Test
    fun signIn() {
        openProfile("Personal")
        openSettings("Sync")
        tap(By.text("Sign in with server address"))
        enterText(textField("Server address"), argument("syncServer"))
        enterText(textField("Username"), argument("syncUsername"))
        enterText(textField("Password"), argument("syncPassword"))
        hideKeyboard()
        tap(By.text("Sign in"))
        find(By.textStartsWith("Up to date"), SIGN_IN_TIMEOUT_MS)
    }

    private companion object {
        // Signing in sends the whole library; a CI runner's emulator is slow at it.
        const val SIGN_IN_TIMEOUT_MS = 3 * 60_000L
    }
}
