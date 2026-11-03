package mihon.core.platform.locale

import mihon.core.common.platform.PlatformFacility
import java.util.Locale

/** The language the app's interface is shown in, as the platform keeps it. */
@PlatformFacility
interface AppLocaleStore {
    /**
     * The app's language: the one the user picked for the app, or else the first of the system's that the app has;
     * null when neither is known.
     */
    val current: Locale?

    /** The language the user picked for the app, as [set] last saved it; null while the app follows the system's. */
    val picked: Locale?

    /**
     * Whether [set] shows the app in its language at once. Otherwise the language applies when the app starts again,
     * which [restartToApply] does now.
     */
    val appliesAtOnce: Boolean

    /** Picks the language [languageTag] names for the app, or the system's for null. */
    fun set(languageTag: String?)

    /** Starts the app again in the language [set] picked. Only called while not [appliesAtOnce]. */
    fun restartToApply()
}
