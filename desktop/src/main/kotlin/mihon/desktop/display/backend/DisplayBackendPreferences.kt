package mihon.desktop.display.backend

import dev.zacsweers.metro.Inject
import mihon.core.metro.BasePreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * The display backend the app starts on, on the base store: it is picked before any profile opens, and one toolkit
 * serves every window of the run.
 */
@Inject
class DisplayBackendPreferences(@BasePreferenceStore store: PreferenceStore) {
    /** Read at start only; where the session doesn't offer native Wayland it is ignored. */
    val useNativeWayland: Preference<Boolean> = store.getBoolean("use_native_wayland", false)
}
