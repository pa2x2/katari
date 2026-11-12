package app.katari.desktop.tray

import app.katari.core.metro.BasePreferenceStore
import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** The tray's settings, on the base store, as they belong to this computer's desktop whichever profile is open. */
@Inject
class TrayPreferences(@BasePreferenceStore store: PreferenceStore) {
    /** Whether closing the window leaves the app running with its icon in the tray, where a tray runs. */
    val keepRunningInTray: Preference<Boolean> = store.getBoolean("tray_keep_running", true)
}
