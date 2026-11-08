package app.katari.desktop.profile

import app.katari.profiles.ui.switcher.ProfileSwitchEffects
import dev.zacsweers.metro.Inject

/**
 * An unlock releases the window's hold on the open profile, as it clears Android's app lock. Desktop has no platform
 * theme to set on a switch as Android sets AppCompat's night mode: a window's only theming is the kit theme, which
 * follows the active profile's UI preferences.
 */
@Inject
class DesktopProfileSwitchEffects(private val lock: ActiveProfileLock) : ProfileSwitchEffects {
    override fun unlocked() = lock.release()

    override fun switched() = Unit
}
