package app.katari.translation.api.host

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext

/** The app's translation settings, which a setting that needs a translation engine sends the user to. */
@PlatformFacility
interface TranslationSettingsDestination {
    /** Opens the settings the way the platform shows a screen from [context], the screen the call comes from. */
    fun openTranslationSettings(context: PlatformContext)
}
