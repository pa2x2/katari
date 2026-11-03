package mihon.desktop.profile

import dev.zacsweers.metro.Inject
import mihon.feature.profiles.ui.switcher.ProfileSwitchEffects

/**
 * Desktop has no app lock for a profile's unlock to clear, and no platform theme to set on a switch as Android sets
 * AppCompat's night mode: a window's only theming is the kit theme, which follows the active profile's UI preferences.
 */
@Inject
class DesktopProfileSwitchEffects : ProfileSwitchEffects {
    override fun unlocked() = Unit

    override fun switched() = Unit
}
