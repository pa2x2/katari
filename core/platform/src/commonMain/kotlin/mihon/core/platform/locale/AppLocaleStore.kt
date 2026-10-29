package mihon.core.platform.locale

import java.util.Locale

/** The language the app's interface is shown in, as the platform keeps it. */
interface AppLocaleStore {
    /**
     * The app's language: the one the user picked for the app, or else the first of the system's that the app has;
     * null when neither is known.
     */
    val current: Locale?
}
