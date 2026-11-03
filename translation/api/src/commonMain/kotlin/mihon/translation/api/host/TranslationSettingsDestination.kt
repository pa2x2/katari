package mihon.translation.api.host

import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext

/** The app's translation settings, which a setting that needs a translation engine sends the user to. */
@PlatformFacility
interface TranslationSettingsDestination {
    /** Opens the settings the way the platform shows a screen from [context], the screen the call comes from. */
    fun openTranslationSettings(context: PlatformContext)
}
