package app.katari.showcase.capture

import android.os.SystemClock
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import app.katari.showcase.capture.ShowcaseDevice.device
import app.katari.showcase.capture.ShowcaseDevice.fail
import app.katari.showcase.capture.ShowcaseDevice.tap

/**
 * Taps the app bar action [title], which a screen shows either as an icon described by its title or as an item of the
 * bar's overflow menu, and may move from one to the other. The icon is looked for first: a screen that is still loading
 * can show its overflow before its other actions.
 */
internal fun tapAction(title: String) {
    val deadline = SystemClock.uptimeMillis() + ACTION_TIMEOUT_MS
    while (SystemClock.uptimeMillis() < deadline) {
        if (device.wait(Until.hasObject(By.desc(title)), ICON_WAIT_MS)) {
            tap(By.desc(title))
            return
        }
        if (device.hasObject(By.res(OVERFLOW))) {
            tap(By.res(OVERFLOW))
            if (tapMenuItem(title)) return
            // Back with no menu open would leave the screen.
            if (device.hasObject(By.res(MENU))) device.pressBack()
        }
    }
    fail("No app bar action is called $title")
}

/** Taps [title] within the open menu, apart from the screen behind it, which may show the same text. */
private fun tapMenuItem(title: String): Boolean {
    val menu = device.wait(Until.findObject(By.res(MENU)), MENU_WAIT_MS) ?: return false
    return try {
        val item = menu.wait(Until.findObject(By.text(title)), MENU_WAIT_MS) ?: return false
        tap(item)
        true
    } catch (_: StaleObjectException) {
        // The menu was still opening; the next attempt opens it again.
        false
    }
}

// The kit's test tags on the app bar's overflow button and on its dropdown menus.
private const val OVERFLOW = "app_bar_overflow"
private const val MENU = "dropdown_menu"

private const val ACTION_TIMEOUT_MS = 30_000L
private const val ICON_WAIT_MS = 2_000L
private const val MENU_WAIT_MS = 2_000L
