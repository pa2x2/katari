package app.katari.desktop.display.scroll.natural

import app.katari.core.metro.BasePreferenceStore
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

/** The Touchpad settings, on the base store, as they belong to this computer's touchpad whichever profile is open. */
@Inject
class TouchpadPreferences(@BasePreferenceStore store: PreferenceStore) {
    val naturalScrolling: Preference<NaturalScrollingChoice> =
        store.getEnum("touchpad_natural_scrolling", NaturalScrollingChoice.AsSystem)
}
